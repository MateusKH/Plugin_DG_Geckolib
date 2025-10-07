package net.nerdypuzzle.geckolib.registry;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import net.mcreator.io.FileIO;
import net.mcreator.io.OS;
import net.mcreator.io.net.WebIO;
import net.mcreator.plugin.MCREvent;
import net.mcreator.plugin.PluginLoader;
import net.mcreator.plugin.PluginUpdateInfo;
import net.mcreator.plugin.events.ui.BlocklyPanelRegisterJSObjects;
import net.mcreator.preferences.PreferencesManager;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.MCreatorApplication;
import net.mcreator.ui.blockly.BlocklyEditorType;
import net.mcreator.ui.blockly.BlocklyPanel;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.component.util.ThreadUtil;
import net.mcreator.ui.dialogs.MCreatorDialog;
import net.mcreator.ui.init.BlocklyJavaScriptsLoader;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.laf.themes.Theme;
import net.mcreator.ui.modgui.ModElementGUI;
import net.mcreator.ui.modgui.ProcedureGUI;
import net.mcreator.ui.variants.modmaker.ModMaker;
import net.mcreator.util.DesktopUtils;
import net.mcreator.util.image.ImageUtils;
import net.mcreator.workspace.elements.VariableTypeLoader;
import net.nerdypuzzle.geckolib.Launcher;
import net.nerdypuzzle.geckolib.element.types.GeckolibElement;
import net.nerdypuzzle.geckolib.parts.JavabridgeReplacement;
import net.nerdypuzzle.geckolib.parts.PluginPanelGeckolib;
import netscape.javascript.JSObject;

/**
 * Controla eventos e integração entre Geckolib e o MCreator.
 */
public class PluginEventTriggers {

    private static final Set<PluginUpdateInfo> pluginUpdates = new HashSet<>();

    private static void checkForPluginUpdates() {
        if (MCreatorApplication.isInternet) {
            pluginUpdates.addAll(Launcher.PLUGIN_INSTANCE.parallelStream().map((plugin) -> {
                try {
                    String updateJSON = WebIO.readURLToString(plugin.getInfo().getUpdateJSONURL());
                    JsonObject updateData = JsonParser.parseString(updateJSON)
                            .getAsJsonObject()
                            .get(plugin.getID()).getAsJsonObject();
                    String version = updateData.get("latest").getAsString();
                    if (!version.equals(plugin.getPluginVersion())) {
                        return new PluginUpdateInfo(
                                plugin, version,
                                updateData.has("changes")
                                        ? updateData.get("changes").getAsJsonArray().asList()
                                        .stream().map(JsonElement::getAsString).toList()
                                        : null
                        );
                    }
                } catch (Exception var4) {
                    var4.printStackTrace();
                }
                return null;
            }).filter(Objects::nonNull).toList());
        }
    }

    public static void dependencyWarning(MCreator mcreator, ModElementGUI modElement) {
        if (!mcreator.getWorkspaceSettings().getDependencies().contains("geckolib")
                && modElement instanceof GeckolibElement) {
            String message = L10N.t("dialog.geckolib.enable_geckolib");
            JOptionPane.showMessageDialog(
                    mcreator,
                    message,
                    L10N.t("dialog.geckolib.error_no_dependency"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void interceptProcedurePanel(MCreator mcreator, ModElementGUI modElement) {
        if (modElement instanceof ProcedureGUI procedure) {
            ThreadUtil.runOnFxThread(() -> {
                try {
                    // Acessa campos privados via reflexão
                    Field panel = ProcedureGUI.class.getDeclaredField("blocklyPanel");
                    panel.setAccessible(true);
                    BlocklyPanel blocklyPanel = (BlocklyPanel) panel.get(procedure);

                    Field loaded = BlocklyPanel.class.getDeclaredField("loaded");
                    loaded.setAccessible(true);
                    loaded.set(blocklyPanel, true);

                    Field engine = BlocklyPanel.class.getDeclaredField("webEngine");
                    engine.setAccessible(true);

                    Field listeners = BlocklyPanel.class.getDeclaredField("changeListeners");
                    listeners.setAccessible(true);
                    List<ChangeListener> listenersList = (List<ChangeListener>) listeners.get(blocklyPanel);

                    JavabridgeReplacement javabridge = new JavabridgeReplacement(mcreator, () ->
                            ThreadUtil.runOnSwingThread(() ->
                                    listenersList.forEach(listener ->
                                            listener.stateChanged(new ChangeEvent(blocklyPanel)))));

                    // === Criação da nova WebView ===
                    WebView browser = new WebView();
                    browser.setContextMenuEnabled(false);
                    Scene scene = new Scene(browser);

                    java.awt.Color bg = Theme.current().getSecondAltBackgroundColor();
                    scene.setFill(javafx.scene.paint.Color.rgb(bg.getRed(), bg.getGreen(), bg.getBlue()));

                    blocklyPanel.setScene(scene);

                    browser.getChildrenUnmodifiable().addListener(
                            (ListChangeListener<Node>) change -> browser.lookupAll(".scroll-bar")
                                    .forEach(bar -> bar.setVisible(false)));

                    WebEngine webEngine = browser.getEngine();
                    webEngine.load(blocklyPanel.getClass()
                            .getResource("/blockly/blockly.html")
                            .toExternalForm());

                    webEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
                        if (newState == Worker.State.SUCCEEDED && webEngine.getDocument() != null) {
                            try {
                                // === Aplica CSS dinâmico ===
                                Element styleNode = webEngine.getDocument().createElement("style");
                                String css = FileIO.readResourceToString("/blockly/css/mcreator_blockly.css");

                                if (PluginLoader.INSTANCE.getResourceAsStream(
                                        "themes/" + Theme.current().getID() + "/styles/blockly.css") != null) {
                                    css += FileIO.readResourceToString(PluginLoader.INSTANCE,
                                            "/themes/" + Theme.current().getID() + "/styles/blockly.css");
                                } else {
                                    css += FileIO.readResourceToString(PluginLoader.INSTANCE,
                                            "/themes/default_dark/styles/blockly.css");
                                }

                                if (PreferencesManager.PREFERENCES.blockly.transparentBackground.get()
                                        && OS.getOS() == OS.WINDOWS) {
                                    Method comps = BlocklyPanel.class.getDeclaredMethod(
                                            "makeComponentsTransparent", Scene.class);
                                    comps.setAccessible(true);
                                    comps.invoke(blocklyPanel, scene);
                                    css += FileIO.readResourceToString("/blockly/css/mcreator_blockly_transparent.css");
                                }

                                if (PreferencesManager.PREFERENCES.blockly.legacyFont.get()) {
                                    css = css.replace("font-family: sans-serif;", "");
                                }

                                Text styleContent = webEngine.getDocument().createTextNode(css);
                                styleNode.appendChild(styleContent);
                                webEngine.getDocument().getDocumentElement()
                                        .getElementsByTagName("head").item(0)
                                        .appendChild(styleNode);

                                // === Java ↔ JS Bridge ===
                                JSObject window = (JSObject) webEngine.executeScript("window");
                                window.setMember("javabridge", javabridge);
                                window.setMember("editorType", BlocklyEditorType.PROCEDURE.registryName());

                                Map<String, Object> domWindowMembers = new HashMap<>();
                                MCREvent.event(new BlocklyPanelRegisterJSObjects(blocklyPanel, domWindowMembers));
                                domWindowMembers.forEach(window::setMember);

                                webEngine.executeScript("var MCR_BLOCKLY_PREF = { "
                                        + "'comments' : " + PreferencesManager.PREFERENCES.blockly.enableComments.get() + ","
                                        + "'renderer' : '" + PreferencesManager.PREFERENCES.blockly.blockRenderer.get().toLowerCase(Locale.ENGLISH) + "',"
                                        + "'collapse' : " + PreferencesManager.PREFERENCES.blockly.enableCollapse.get() + ","
                                        + "'trashcan' : " + PreferencesManager.PREFERENCES.blockly.enableTrashcan.get() + ","
                                        + "'maxScale' : " + PreferencesManager.PREFERENCES.blockly.maxScale.get() / 100.0 + ","
                                        + "'minScale' : " + PreferencesManager.PREFERENCES.blockly.minScale.get() / 100.0 + ","
                                        + "'scaleSpeed' : " + PreferencesManager.PREFERENCES.blockly.scaleSpeed.get() / 100.0 + ","
                                        + "'saturation' :" + PreferencesManager.PREFERENCES.blockly.colorSaturation.get() / 100.0 + ","
                                        + "'value' :" + PreferencesManager.PREFERENCES.blockly.colorValue.get() / 100.0
                                        + " };");

                                // === Carregamento de scripts Blockly ===
                                webEngine.executeScript(FileIO.readResourceToString("/jsdist/blockly_compressed.js"));
                                webEngine.executeScript(FileIO.readResourceToString(
                                        "/jsdist/msg/" + L10N.getBlocklyLangName() + ".js"));
                                webEngine.executeScript(FileIO.readResourceToString("/jsdist/blocks_compressed.js"));
                                webEngine.executeScript(FileIO.readResourceToString("/blockly/js/mcreator_blockly.js"));

                                for (String script : BlocklyJavaScriptsLoader.INSTANCE.getScripts())
                                    webEngine.executeScript(script);

                                webEngine.executeScript(VariableTypeLoader.INSTANCE.getVariableBlocklyJS());

                                Field tasks = BlocklyPanel.class.getDeclaredField("runAfterLoaded");
                                tasks.setAccessible(true);
                                List<Runnable> tasklist = (List<Runnable>) tasks.get(blocklyPanel);
                                tasklist.forEach(ThreadUtil::runOnFxThread);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    engine.set(blocklyPanel, webEngine);
                    panel.set(procedure, blocklyPanel);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void forceCheckUpdates(MCreator mcreator) {
        checkForPluginUpdates();
        Collection<PluginUpdateInfo> pluginUpdateInfos = pluginUpdates;
        if (!pluginUpdateInfos.isEmpty()) {
            JPanel pan = new JPanel(new BorderLayout(10, 15));
            JPanel plugins = new JPanel(new GridLayout(0, 1, 10, 10));
            pan.add("North", new JScrollPane(PanelUtils.pullElementUp(plugins)));
            pan.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            pan.setPreferredSize(new Dimension(560, 250));

            for (PluginUpdateInfo pluginUpdateInfo : pluginUpdateInfos) {
                StringBuilder usb = new StringBuilder(L10N.t(
                        "dialog.plugin_update_notify.version_message",
                        pluginUpdateInfo.plugin().getInfo().getName(),
                        pluginUpdateInfo.plugin().getInfo().getVersion(),
                        pluginUpdateInfo.newVersion()));

                if (pluginUpdateInfo.recentChanges() != null) {
                    usb.append("<br>")
                            .append(L10N.t("dialog.plugin_update_notify.changes_message"))
                            .append("<ul>");
                    for (String change : pluginUpdateInfo.recentChanges())
                        usb.append("<li>").append(change).append("</li>");
                }

                JLabel label = new JLabel(usb.toString());
                JButton update = L10N.button("dialog.plugin_update_notify.update");
                update.addActionListener(e ->
                        DesktopUtils.browseSafe("https://mcreator.net/node/" +
                                pluginUpdateInfo.plugin().getInfo().getPluginPageID()));
                plugins.add(PanelUtils.westAndEastElement(label, PanelUtils.join(new Component[]{update})));
            }

            MCreatorDialog dialog = new MCreatorDialog(mcreator,
                    L10N.t("dialog.plugin_update_notify.update_title"));
            dialog.setSize(700, 200);
            dialog.setLocationRelativeTo(mcreator);
            dialog.setModal(true);

            JButton close = L10N.button("dialog.plugin_update_notify.close");
            close.addActionListener(e -> dialog.setVisible(false));
            dialog.add("Center",
                    PanelUtils.centerAndSouthElement(pan, PanelUtils.join(new Component[]{close})));
            dialog.setVisible(true);
        }
    }

    public static void modifyMenus(MCreator mcreator) {
        JMenu geckolib = L10N.menu("menubar.geckolib");
        geckolib.setMnemonic('R');
        geckolib.setIcon(new ImageIcon(ImageUtils.resizeAA(UIRES.get("16px.geckolibicon").getImage(), 17, 17)));
        geckolib.add(Launcher.ACTION_REGISTRY.importGeckoLibModel);
        geckolib.add(Launcher.ACTION_REGISTRY.importDisplaySettings);
        geckolib.addSeparator();
        geckolib.add(Launcher.ACTION_REGISTRY.convertion_to_geckolib);
        geckolib.add(Launcher.ACTION_REGISTRY.convertion_from_geckolib);
        geckolib.addSeparator();
        geckolib.add(Launcher.ACTION_REGISTRY.tutorial);

        if (mcreator instanceof ModMaker modmaker) {
            PluginPanelGeckolib panel = new PluginPanelGeckolib(modmaker.getWorkspacePanel());
            panel.setOpaque(false);
            modmaker.getWorkspacePanel().resourcesPan.addResourcesTab("geckolib", panel);
            mcreator.getMainMenuBar().add(geckolib);
        }

        forceCheckUpdates(mcreator);
    }
}