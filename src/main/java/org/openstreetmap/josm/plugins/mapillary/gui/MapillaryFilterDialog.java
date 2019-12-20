// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary.gui;

import static java.awt.FlowLayout.LEFT;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang3.StringUtils;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.widgets.DisableShortcutsOnFocusGainedTextField;
import org.openstreetmap.josm.plugins.mapillary.MapillaryAbstractImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryDataListener;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImportedImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryLayer;
import org.openstreetmap.josm.plugins.mapillary.model.ImageDetection;
import org.openstreetmap.josm.plugins.mapillary.model.UserProfile;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * ToggleDialog that lets you filter the images that are being shown.
 *
 * @author nokutu
 * @see MapillaryFilterChooseSigns
 */
public final class MapillaryFilterDialog extends ToggleDialog implements MapillaryDataListener {

  private static final long serialVersionUID = -4192029663670922103L;
  public static final String YYYY_MM_DD = "yyyy/MM/dd";
  public static final ZoneId UTC = ZoneId.of("UTC");

  private static MapillaryFilterDialog instance;

  private static final String[] TIME_LIST = {tr("Years"), tr("Months"), tr("Days")};

  private static final long[] TIME_FACTOR = new long[]{
    31_536_000_000L, // = 365 * 24 * 60 * 60 * 1000 = number of ms in a year
    2_592_000_000L, // = 30 * 24 * 60 * 60 * 1000 = number of ms in a month
    86_400_000 // = 24 * 60 * 60 * 1000 = number of ms in a day
  };

  private final JCheckBox filterByDateCheckbox;
  private final JCheckBox filterByDateRangeCheckbox;
  private final JTextField fromDate;
  private final JTextField toDate;

  /**
   * Spinner to choose the range of dates.
   */
  private final SpinnerNumberModel spinnerModel;

  private final JCheckBox imported = new JCheckBox(tr("Imported images"));
  private final JCheckBox downloaded = new JCheckBox(new DownloadCheckBoxAction());
  private final JCheckBox onlySigns = new JCheckBox(new OnlySignsAction());
  private final JComboBox<String> time;
  private final JTextField user;
  private final JTextField organizationKey;

  private final JButton signChooser = new JButton(new SignChooserAction());
  private final JRadioButton buttonBoth;
  private final JRadioButton button360;
  private final JRadioButton buttonNormal;

  private MapillaryFilterDialog() {
    super(tr("Mapillary filter"), "mapillary-filter", tr("Open Mapillary filter dialog"), null, 200,
      false, MapillaryPreferenceSetting.class);

    this.signChooser.setEnabled(false);
    JPanel signChooserPanel = new JPanel();
    signChooserPanel.setLayout(new FlowLayout(LEFT));
    signChooserPanel.add(this.signChooser);

    JPanel fromPanel = new JPanel();
    fromPanel.setLayout(new FlowLayout(LEFT));
    filterByDateCheckbox = new JCheckBox(tr("Not older than: "));
    fromPanel.add(filterByDateCheckbox);
    this.spinnerModel = new SpinnerNumberModel(1.0, 0, 10000, .1);
    JSpinner spinner = new JSpinner(spinnerModel);
    spinner.setEnabled(false);
    fromPanel.add(spinner);
    time = new JComboBox<>(TIME_LIST);
    time.setEnabled(false);
    fromPanel.add(this.time);

    JPanel dateRangePanel = new JPanel();
    dateRangePanel.setLayout(new FlowLayout(LEFT));
    filterByDateRangeCheckbox = new JCheckBox();
    dateRangePanel.add(filterByDateRangeCheckbox);
    dateRangePanel.add(new JLabel("From"));
    fromDate = new DisableShortcutsOnFocusGainedTextField(10);
    dateRangePanel.add(fromDate);
    dateRangePanel.add(new JLabel("To"));
    toDate = new DisableShortcutsOnFocusGainedTextField(10);
    dateRangePanel.add(toDate);
    dateRangePanel.add(new JLabel(YYYY_MM_DD + " CET time"));

    filterByDateCheckbox.addItemListener(itemE -> {
      boolean fromTimeCriterionActive = filterByDateCheckbox.isSelected();
      spinner.setEnabled(fromTimeCriterionActive);
      time.setEnabled(fromTimeCriterionActive);
      if (fromTimeCriterionActive) {
        filterByDateRangeCheckbox.setSelected(false);
      }
    });

    filterByDateRangeCheckbox.addItemListener(itemEvent -> {
        boolean dateRangeCriterionActive = filterByDateRangeCheckbox.isSelected();
        fromDate.setEnabled(dateRangeCriterionActive);
        toDate.setEnabled(dateRangeCriterionActive);
        if (dateRangeCriterionActive) {
          filterByDateCheckbox.setSelected(false);
        }
      }
    );

    JPanel userSearchPanel = new JPanel();
    userSearchPanel.setLayout(new FlowLayout(LEFT));

    this.user = new DisableShortcutsOnFocusGainedTextField(10);
    this.user.addActionListener(new UpdateAction());
    userSearchPanel.add(new JLabel(tr("User")));
    userSearchPanel.add(this.user);

    this.organizationKey = new DisableShortcutsOnFocusGainedTextField(10);
    this.organizationKey.addActionListener(new UpdateAction());
    userSearchPanel.add(new JLabel(tr("Organization key")));
    userSearchPanel.add(this.organizationKey);

    this.imported.setSelected(true);
    this.downloaded.setSelected(true);

    JPanel panoramicPanel = new JPanel();
    panoramicPanel.setLayout(new FlowLayout(LEFT));
    ButtonGroup buttonGroupPanoramic = new ButtonGroup();
    buttonBoth = new JRadioButton("Both");
    buttonGroupPanoramic.add(buttonBoth);
    panoramicPanel.add(buttonBoth);
    button360 = new JRadioButton("360°");
    buttonGroupPanoramic.add(button360);
    panoramicPanel.add(button360);
    buttonNormal = new JRadioButton("Normal");
    buttonGroupPanoramic.add(buttonNormal);
    panoramicPanel.add(buttonNormal);

    JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.LINE_START;
    panel.add(this.downloaded, c);
    c.gridx = 1;
    panel.add(this.imported, c);
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 2;
    panel.add(fromPanel, c);
    c.gridy = 2;
    panel.add(dateRangePanel, c);
    c.gridy = 3;
    panel.add(userSearchPanel, c);
    c.gridwidth = 1;
    c.gridy = 4;
    panel.add(this.onlySigns, c);
    c.gridx = 1;
    panel.add(signChooserPanel, c);
    c.gridx = 0;
    c.gridy = 5;
    panel.add(panoramicPanel, c);

    createLayout(panel, true, Arrays.asList(new SideButton(new UpdateAction()), new SideButton(new ResetAction())));
  }

  /**
   * @return the unique instance of the class.
   */
  public static synchronized MapillaryFilterDialog getInstance() {
    if (instance == null)
      instance = new MapillaryFilterDialog();
    return instance;
  }

  @Override
  public void imagesAdded() {
    refresh();
  }

  @Override
  public void selectedImageChanged(MapillaryAbstractImage oldImage, MapillaryAbstractImage newImage) {
    // Do nothing when image selection changed
  }

  /**
   * Resets the dialog to its default state.
   */
  public void reset() {
    this.imported.setSelected(true);
    this.downloaded.setSelected(true);
    this.onlySigns.setEnabled(true);
    this.onlySigns.setSelected(false);
    this.user.setText("");
    this.organizationKey.setText("");
    this.time.setSelectedItem(TIME_LIST[0]);
    this.spinnerModel.setValue(1);
    this.filterByDateCheckbox.setSelected(false);
    this.filterByDateRangeCheckbox.setSelected(false);
    this.fromDate.setText("");
    this.toDate.setText("");
    this.buttonBoth.setSelected(true);
    refresh();
  }

  /**
   * Applies the selected filter.
   */
  public synchronized void refresh() {
    final boolean layerVisible = MapillaryLayer.hasInstance() && MapillaryLayer.getInstance().isVisible();
    final boolean imported = this.imported.isSelected();
    final boolean downloaded = this.downloaded.isSelected();
    final boolean timeFilter = filterByDateCheckbox.isSelected();
    final boolean dateRangeFilter = filterByDateRangeCheckbox.isSelected();
    final boolean panoramaFilter = !buttonBoth.isSelected();
    final boolean onlySigns = this.onlySigns.isSelected();

    // This predicate returns true is the image should be made invisible
    Predicate<MapillaryAbstractImage> shouldHide =
      img -> {
        if (!layerVisible) {
          return true;
        }
        if (timeFilter && checkValidTime(img)) {
          return true;
        }
        if (dateRangeFilter && checkNotBetweenDate(img)) {
          return true;
        }
        if (!imported && img instanceof MapillaryImportedImage) {
          return true;
        }
        if (panoramaFilter) {
          if (img.isPanorama() == buttonNormal.isSelected()) {
            return true;
          }
        }
        if (img instanceof MapillaryImage) {
          if (!downloaded) {
            return true;
          }
          if (onlySigns && (((MapillaryImage) img).getDetections().isEmpty() || !checkSigns((MapillaryImage) img))) {
            return true;
          }
          UserProfile userProfile = ((MapillaryImage) img).getUser();
          if (!"".equals(user.getText()) && (userProfile == null || !user.getText().equals(userProfile.getUsername()))) {
            return true;
          }
          String imageOrganizationKey = ((MapillaryImage) img).getOrganizationKey();
          if (!"".equals(organizationKey.getText()) && (imageOrganizationKey == null || !organizationKey.getText().equals(imageOrganizationKey))) {
            return true;
          }
        }
        return false;
      };

    if (MapillaryLayer.hasInstance()) {
      MapillaryLayer.getInstance().getData().getImages().parallelStream().forEach(img -> img.setVisible(!shouldHide.test(img)));
    }

    MapillaryLayer.invalidateInstance();
  }

  private boolean checkValidTime(MapillaryAbstractImage img) {
    Long currentTime = currentTime();
    for (int i = 0; i < 3; i++) {
      if (TIME_LIST[i].equals(time.getSelectedItem()) &&
        img.getCapturedAt() < currentTime - spinnerModel.getNumber().doubleValue() * TIME_FACTOR[i]) {
        return true;
      }
    }
    return false;
  }

  private boolean checkNotBetweenDate(MapillaryAbstractImage img) {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YYYY_MM_DD);
    String fromDateAsString = fromDate.getText();
    boolean isAfter = true;
    long capturedAt = img.getCapturedAt();
    if (capturedAt == 0) {
      return true;
    }
    if (StringUtils.isNotEmpty(fromDateAsString)) {
      try {
        LocalDate fromDate = LocalDate.parse(fromDateAsString, formatter);
        long fromInstant = getInstant(fromDate);
        isAfter = capturedAt >= fromInstant;
      } catch (DateTimeParseException e) {
        Logging.log(Logging.LEVEL_WARN, "Exception while trying to parse the from date in the image filter", e);
      }
    }
    String toDateAsString = toDate.getText();
    boolean isBefore = true;
    if (StringUtils.isNotEmpty(toDateAsString)) {
      try {
        LocalDate toDate = LocalDate.parse(toDateAsString, formatter);
        long toInstant = getInstant(toDate);
        isBefore = capturedAt <= toInstant;
      } catch (DateTimeParseException e) {
        Logging.log(Logging.LEVEL_WARN, "Exception while trying to parse the from date in the image filter", e);
      }
    }
    return !isAfter || !isBefore;
  }

  private long getInstant(LocalDate fromDate) {
    return fromDate.atStartOfDay(UTC).toInstant().toEpochMilli();
  }

  /**
   * Checks if the image fulfills the sign conditions.
   *
   * @param img The {@link MapillaryAbstractImage} object that is going to be
   *            checked.
   * @return {@code true} if it fulfills the conditions; {@code false}
   * otherwise.
   */
  private static boolean checkSigns(MapillaryImage img) {
    for (int i = 0; i < MapillaryFilterChooseSigns.SIGN_TAGS.length; i++) {
      if (checkSign(img, MapillaryFilterChooseSigns.getInstance().signCheckboxes[i], MapillaryFilterChooseSigns.SIGN_TAGS[i]))
        return true;
    }
    return false;
  }

  private static boolean checkSign(MapillaryImage img, JCheckBox signCheckBox, String signTag) {
    boolean contains = false;
    for (ImageDetection detection : img.getDetections()) {
      if (Pattern.compile(signTag).matcher(detection.getValue()).find()) {
        contains = true;
      }
    }
    return contains == signCheckBox.isSelected() && contains;
  }

  private static long currentTime() {
    Calendar cal = Calendar.getInstance();
    return cal.getTimeInMillis();
  }

  /**
   * Destroys the unique instance of the class.
   */
  public static synchronized void destroyInstance() {
    instance = null;
  }

  private class DownloadCheckBoxAction extends AbstractAction {

    private static final long serialVersionUID = 4672634002899519496L;

    DownloadCheckBoxAction() {
      putValue(NAME, tr("Downloaded images"));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      onlySigns.setEnabled(downloaded.isSelected());
    }
  }

  private static class UpdateAction extends AbstractAction {

    private static final long serialVersionUID = -7417238601979689863L;

    UpdateAction() {
      putValue(NAME, tr("Update"));
      new ImageProvider("dialogs", "refresh").getResource().attachImageIcon(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      MapillaryFilterDialog.getInstance().refresh();
    }
  }

  private static class ResetAction extends AbstractAction {
    private static final long serialVersionUID = 1178261778165525040L;

    ResetAction() {
      putValue(NAME, tr("Reset"));
      new ImageProvider("preferences", "reset").getResource().attachImageIcon(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      MapillaryFilterDialog.getInstance().reset();
    }
  }

  private class OnlySignsAction extends AbstractAction {

    private static final long serialVersionUID = -2937440338019185723L;

    OnlySignsAction() {
      putValue(NAME, tr("Only images with signs"));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      signChooser.setEnabled(onlySigns.isSelected());
    }
  }

  /**
   * Opens a new window where you can specifically filter signs.
   *
   * @author nokutu
   */
  private static class SignChooserAction extends AbstractAction {

    private static final long serialVersionUID = 8706299665735930148L;

    SignChooserAction() {
      putValue(NAME, tr("Choose signs"));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      final JOptionPane pane = new JOptionPane(
        MapillaryFilterChooseSigns.getInstance(),
        JOptionPane.PLAIN_MESSAGE,
        JOptionPane.OK_CANCEL_OPTION
      );
      JDialog dlg = pane.createDialog(MainApplication.getMainFrame(), tr("Choose signs"));
      dlg.setVisible(true);
      if ((int) pane.getValue() == JOptionPane.OK_OPTION)
        MapillaryFilterDialog.getInstance().refresh();
      dlg.dispose();
    }
  }
}
