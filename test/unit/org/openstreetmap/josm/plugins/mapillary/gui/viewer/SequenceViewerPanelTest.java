package org.openstreetmap.josm.plugins.mapillary.gui.viewer;

import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.junit.Test;

public class SequenceViewerPanelTest {

  @Test
  public void getResourceUrl() {
    URL url = SequenceViewerPanel.getMapillaryJsSnippetUrl();
    assertNotNull(url);
  }

}
