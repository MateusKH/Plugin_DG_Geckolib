package net.nerdypuzzle.geckolib.parts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import net.mcreator.ui.component.TransparentToolBar;
import net.mcreator.ui.component.util.ComponentUtils;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.workspace.IReloadableFilterable;
import net.mcreator.ui.workspace.WorkspacePanel;
import net.mcreator.util.StringUtils;
import net.nerdypuzzle.geckolib.Launcher;

public class PluginPanelGeckolib extends JPanel implements IReloadableFilterable {

    private final WorkspacePanel workspacePanel;
    private final FilterModel listmodel = new FilterModel();
    private final JList<File> modelList;

    public PluginPanelGeckolib(final WorkspacePanel workspacePanel) {
        super(new BorderLayout());
        this.workspacePanel = workspacePanel;
        this.modelList = new JList<>(this.listmodel);

        setOpaque(false);
        modelList.setOpaque(false);
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modelList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        modelList.setVisibleRowCount(-1);
        modelList.setCellRenderer(new Render());

        // Mostra o nome do modelo sob o cursor
        modelList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                int idx = modelList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    File item = modelList.getModel().getElementAt(idx);
                    if (item != null)
                        workspacePanel.getMCreator().getStatusBar().setMessage(item.getName());
                }
            }
        });

        // ScrollPane configurado
        JScrollPane sp = new JScrollPane(modelList);
        sp.setOpaque(false);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(11);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        add("Center", sp);

        // Barra de botões
        TransparentToolBar bar = new TransparentToolBar();
        bar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 0));

        // Importar modelo Geckolib
        JButton imp1 = L10N.button("action.workspace.resources.import_geckolib_model");
        imp1.setIcon(UIRES.get("16px.importgeckolibmodel"));
        imp1.setContentAreaFilled(false);
        imp1.setOpaque(false);
        imp1.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        imp1.addActionListener(e -> {
            Launcher.ACTION_REGISTRY.importGeckoLibModel.doAction();
            reloadElements();
        });
        ComponentUtils.deriveFont(imp1, 12.0F);
        bar.add(imp1);

        // Importar display settings
        JButton imp2 = L10N.button("action.workspace.resources.import_display_settings");
        imp2.setIcon(UIRES.get("16px.importgeckolibmodel"));
        imp2.setContentAreaFilled(false);
        imp2.setOpaque(false);
        imp2.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        imp2.addActionListener(e -> {
            Launcher.ACTION_REGISTRY.importDisplaySettings.doAction();
            reloadElements();
        });
        ComponentUtils.deriveFont(imp2, 12.0F);
        bar.add(imp2);

        // Botão de exclusão
        JButton del = L10N.button("workspace.3dmodels.delete_selected");
        del.setIcon(UIRES.get("16px.delete"));
        del.setOpaque(false);
        del.setContentAreaFilled(false);
        del.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        bar.add(del);
        del.addActionListener(e -> deleteCurrentlySelected());

        // Delete com tecla DEL
        modelList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE)
                    deleteCurrentlySelected();
            }
        });

        add("North", bar);
    }

    private void deleteCurrentlySelected() {
        File file = modelList.getSelectedValue();
        if (file == null)
            return;

        String animFilePath = file.getAbsolutePath()
                .replace("\\geo\\", "\\animations\\")
                .replace(".geo.", ".animation.");

        int n = JOptionPane.showConfirmDialog(
                workspacePanel.getMCreator(),
                L10N.t("workspace.3dmodels.delete_confirm_message"),
                L10N.t("common.confirmation"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (n == JOptionPane.YES_OPTION) {
            file.delete();
            File animFile = new File(animFilePath);
            if (animFile.exists())
                animFile.delete();
            reloadElements();
            workspacePanel.getMCreator().getStatusBar().setMessage(
                    L10N.t("workspace.3dmodels.deleted_successfully"));
        }
    }

    @Override
    public void reloadElements() {
        listmodel.removeAllElements();
        List<File> geomodels = PluginModelActions.getGeomodels(workspacePanel.getMCreator());
        List<File> displays = PluginModelActions.getDisplaysettings(workspacePanel.getMCreator());

        for (File model : geomodels)
            listmodel.addElement(model);
        for (File display : displays)
            listmodel.addElement(display);

        refilterElements();
    }

    @Override
    public void refilterElements() {
        listmodel.refilter();
    }

    private class FilterModel extends DefaultListModel<File> {
        private final List<File> items = new ArrayList<>();
        private final List<File> filterItems = new ArrayList<>();

        @Override
        public int indexOf(Object elem) {
            return elem instanceof File ? filterItems.indexOf(elem) : -1;
        }

        @Override
        public File getElementAt(int index) {
            return (!filterItems.isEmpty() && index < filterItems.size()) ? filterItems.get(index) : null;
        }

        @Override
        public int getSize() {
            return filterItems.size();
        }

        @Override
        public void addElement(File o) {
            items.add(o);
            refilter();
        }

        @Override
        public void removeAllElements() {
            super.removeAllElements();
            items.clear();
            filterItems.clear();
        }

        @Override
        public boolean removeElement(Object a) {
            if (a instanceof File) {
                items.remove(a);
                filterItems.remove(a);
            }
            return super.removeElement(a);
        }

        void refilter() {
            filterItems.clear();
            String term = "";

            // 🔍 Acesso seguro ao campo protegido "search"
            try {
                var field = workspacePanel.getClass().getSuperclass().getDeclaredField("search");
                field.setAccessible(true);
                JTextField searchField = (JTextField) field.get(workspacePanel);
                if (searchField != null)
                    term = searchField.getText().toLowerCase(Locale.ENGLISH);
            } catch (Exception ignored) {}

            final String searchTerm = term;
            filterItems.addAll(items.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> item.getName().toLowerCase(Locale.ENGLISH).contains(searchTerm))
                    .toList());

            if (workspacePanel.sortName.isSelected())
                filterItems.sort(Comparator.comparing(File::getName));

            if (workspacePanel.desc.isSelected())
                Collections.reverse(filterItems);

            fireContentsChanged(this, 0, getSize());
        }
    }

    static class Render extends JLabel implements ListCellRenderer<File> {
        @Override
        public Component getListCellRendererComponent(
                JList<? extends File> list, File ma, int index, boolean isSelected, boolean cellHasFocus) {

            setOpaque(true);
            setBackground(isSelected
                    ? UIManager.getColor("MCreatorLAF.LIGHT_ACCENT")
                    : new Color(0, 0, 0, 0));

            setText(StringUtils.abbreviateString(ma.getName(), 13));
            setToolTipText(ma.getName());
            ComponentUtils.deriveFont(this, 11.0F);
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setHorizontalAlignment(SwingConstants.CENTER);
            setIcon(UIRES.get("model.geckolib"));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            return this;
        }
    }
}