package org.openstreetmap.josm.plugins.mapillary.gui.viewer;

import java.awt.BorderLayout;
import java.net.URL;

import javax.swing.JPanel;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.I18n;

public final class SequenceViewerPanel extends ToggleDialog {

  private static SequenceViewerPanel instance;

  private SequenceViewerPanel() {
    super(
      I18n.tr("Mapillary viewer"),
      "mapillary-info",
      I18n.tr("Displays a viewer for the Mapillary sequences"),
      null,
      150
    );

    initLayout();
  }

  private void initLayout() {
    JPanel root = new JPanel(new BorderLayout());

    Browser browser = new Browser();
    BrowserView view = new BrowserView(browser);
    root.add(view, BorderLayout.CENTER);
    browser.loadHTML(SNIPPET);
    createLayout(root, false, null);
  }


 /* private static final String SNIPPET = "<!DOCTYPE html>\n" +
    "<html>\n" +
    "<head>\n" +
    "  <script src='https://unpkg.com/mapillary-js@2.14.1/dist/mapillary.min.js'></script>\n" +
    "  <link href='https://unpkg.com/mapillary-js@2.14.1/dist/mapillary.min.css' rel='stylesheet'/>\n" +
    "</head>\n" +
    "\n" +
    "<body>\n" +
    "<div id='mly' style='width: 640px; height: 480px;'></div>\n" +
    "\n" +
    "<script>\n" +
    "        var mly = new Mapillary.Viewer(\n" +
    "            'mly',\n" +
    "            'MkJKbDA0bnZuZlcxeTJHTmFqN3g1dzo0ZmYxN2MzMTRlYzM1M2E2',\n" +
    "            'RJLsRCfp01AJFudwX7V9rQ'\n" +
    "        );\n" +
    "\n" +
    "</script>\n" +
    "</body>\n" +
    "</html>\n";
    */

  private static final String SNIPPET = "<!DOCTYPE html>\n" +
    "<html>\n" +
    "<head>\n" +
    "    <meta charset='utf-8' />\n" +
    "    <title></title>\n" +
    "    <meta name='viewport' content='initial-scale=1,maximum-scale=1,user-scalable=no' />\n" +
    "\n" +
    "    <script src='https://unpkg.com/mapillary-js@2.15.0/dist/mapillary.min.js'></script>\n" +
    "    <link href='https://unpkg.com/mapillary-js@2.15.0/dist/mapillary.min.css' rel='stylesheet' />\n" +
    "\n" +
    "    <style>\n" +
    "        html, body { margin: 0; padding: 0; height: 100%; }\n" +
    "        #mly { height: 100%; }\n" +
    "        #message {\n" +
    "            text-align: center;\n" +
    "            width: 100%;\n" +
    "            position: absolute;\n" +
    "            top: 60px;\n" +
    "            padding: 4px 0;\n" +
    "            background-color: white;\n" +
    "        }\n" +
    "    </style>\n" +
    "</head>\n" +
    "\n" +
    "<body>\n" +
    "    <div id='mly'></div>\n" +
    "    <div id='message'></div>\n" +
    "\n" +
    "    <script>\n" +
    "        function setMessage(message) {\n" +
    "            var messageContainer = document.getElementById('message');\n" +
    "            messageContainer.innerHTML = message;\n" +
    "        }\n" +
    "\n" +
    "        var componentOptions = null;\n" +
    "\n" +
    "        if (Mapillary.isSupported()) {\n" +
    "            // Enable or disable any components, e.g. tag and popup which requires WebGL support\n" +
    "            // or use the default components by not supplying any component options.\n" +
    "            componentOptions = { /* Default options */ };\n" +
    "\n" +
    "            setMessage(\"MapillaryJS is fully supported by your browser.\");\n" +
    "            console.log(\"MapillaryJS is fully supported by your browser.\");\n" +
    "        } else if (Mapillary.isFallbackSupported()) {\n" +
    "            // On top of the disabled components below, also the popup, marker and tag\n" +
    "            // components require WebGL support and should not be enabled (they are\n" +
    "            // disabled by default so does not need to be specified below).\n" +
    "            componentOptions = {\n" +
    "                // Disable components requiring WebGL support\n" +
    "                direction: false,\n" +
    "                imagePlane: false,\n" +
    "                keyboard: false,\n" +
    "                mouse: false,\n" +
    "                sequence: false,\n" +
    "\n" +
    "                // Enable fallback components\n" +
    "                image: true,\n" +
    "                navigation: true,\n" +
    "            }\n" +
    "\n" +
    "            console.log(\"MapillaryJS fallback functionality is supported by your browser.\");\n" +
    "            setMessage(\"MapillaryJS fallback functionality is supported by your browser.\");\n" +
    "        } else {\n" +
    "            // Handle the fact that MapillaryJS is not supported in a way that is\n" +
    "            // appropriate for your application.\n" +
    "            console.log(\"MapillaryJS is not supported by your browser.\");\n" +
    "            setMessage(\"MapillaryJS is not supported by your browser.\");\n" +
    "        }\n" +
    "\n" +
    "        if (!!componentOptions) {\n" +
    "            // Deactivate cover without interaction needed.\n" +
    "            componentOptions.cover = false;\n" +
    "\n" +
    "            var mly = new Mapillary.Viewer(\n" +
    "                'mly',\n" +
    "                // Replace this with your own client ID from mapillary.com\n" +
    "                'MkJKbDA0bnZuZlcxeTJHTmFqN3g1dzo0ZmYxN2MzMTRlYzM1M2E2',\n" +
    "                'RJLsRCfp01AJFudwX7V9rQ',\n" +
    "                { component: componentOptions });\n" +
    "\n" +
    "            // Viewer size is dynamic so resize should be called every time the window size changes\n" +
    "            window.addEventListener(\"resize\", function() { mly.resize(); });\n" +
    "        }\n" +
    "    </script>\n" +
    "</body>\n" +
    "</html>\n";


  protected static URL getMapillaryJsSnippetUrl() {
    return SequenceViewerPanel.class.getResource("test.html");
  }

  public static SequenceViewerPanel getInstance() {
    synchronized (SequenceViewerPanel.class) {
      if (instance == null) {
        instance = new SequenceViewerPanel();
      }
      return instance;
    }
  }

}
