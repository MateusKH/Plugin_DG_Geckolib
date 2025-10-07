package net.nerdypuzzle.geckolib.parts;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.mcreator.blockly.data.BlocklyLoader;
import net.mcreator.blockly.data.Dependency;
import net.mcreator.blockly.data.ExternalTrigger;
import net.mcreator.blockly.java.BlocklyVariables;
import net.mcreator.element.types.LivingEntity;
import net.mcreator.element.types.Procedure;
import net.mcreator.minecraft.DataListEntry;
import net.mcreator.minecraft.DataListLoader;
import net.mcreator.minecraft.ElementUtil;
import net.mcreator.minecraft.MCItem;
import net.mcreator.minecraft.MinecraftImageGenerator;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.dialogs.AIConditionEditor;
import net.mcreator.ui.dialogs.DataListSelectorDialog;
import net.mcreator.ui.dialogs.MCItemSelectorDialog;
import net.mcreator.ui.dialogs.StringSelectorDialog;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.minecraft.states.PropertyData;
import net.mcreator.util.image.ImageUtils;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.elements.VariableType;
import net.mcreator.workspace.elements.VariableTypeLoader;
import net.nerdypuzzle.geckolib.element.types.AnimatedEntity;
import netscape.javascript.JSObject;

/**
 * Ponte entre Blockly JS e o ambiente Java.
 * Compatível com MCreator 2025.3 (Java 21).
 */
public final class JavabridgeReplacement {

    private static final Logger LOG = LogManager.getLogger("Blockly JS Bridge");

    private final Runnable blocklyEvent;
    private final MCreator mcreator;

    public JavabridgeReplacement(MCreator mcreator, Runnable blocklyEvent) {
        this.blocklyEvent = blocklyEvent;
        this.mcreator = mcreator;
        List<ExternalTrigger> triggers = BlocklyLoader.INSTANCE.getExternalTriggerLoader().getExternalTriggers();
        triggers.forEach(this::addExternalTrigger);
    }

    /** Chamado pelo JavaScript para executar o evento Blockly. */
    @SuppressWarnings("unused")
    public void triggerEvent() {
        blocklyEvent.run();
    }

    /** Retorna um item em formato Base64 (ícone do Minecraft). */
    @SuppressWarnings("unused")
    public String getMCItemURI(String name) {
        ImageIcon base = new ImageIcon(ImageUtils.resize(MinecraftImageGenerator.generateItemSlot(), 36, 36));
        ImageIcon image;
        if (name != null && !name.isEmpty() && !"null".equals(name))
            image = ImageUtils.drawOver(base, MCItem.getBlockIconBasedOnName(mcreator.getWorkspace(), name), 2, 2, 32, 32);
        else
            image = base;

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(ImageUtils.toBufferedImage(image.getImage()), "PNG", os);
            return "data:image/png;base64," + Base64.getMimeEncoder().encodeToString(os.toByteArray());
        } catch (Exception ioe) {
            LOG.error("Erro ao gerar imagem base64: {}", ioe.getMessage(), ioe);
            return "";
        }
    }

    /** Abre seletor de item e retorna o nome ao callback JS. */
    @SuppressWarnings("unused")
    public void openMCItemSelector(String type, JSObject callback) {
        AtomicReference<String> ref = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                MCItem selected = MCItemSelectorDialog.openSelectorDialog(mcreator,
                        "allblocks".equals(type) ? ElementUtil::loadBlocks : ElementUtil::loadBlocksAndItems);
                ref.set(selected != null ? selected.getName() : null);
            });
        } catch (Exception e) {
            LOG.error("Erro ao abrir seletor de item: {}", e.getMessage(), e);
        }
        callback.call("callback", ref.get());
    }

    /** Abre editor de condição AI (sem JavaFX). */
    @SuppressWarnings("unused")
    public void openAIConditionEditor(String data, JSObject callback) {
        AtomicReference<String> ref = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                List<String> retval = AIConditionEditor.open(mcreator, data.split(","));
                ref.set(StringUtils.join(retval, ','));
            });
        } catch (Exception e) {
            LOG.error("Erro ao abrir AIConditionEditor: {}", e.getMessage(), e);
        }
        callback.call("callback", ref.get());
    }

    /** Abre seletor genérico de DataListEntry. */
    private String[] openDataListEntrySelector(Function<Workspace, List<DataListEntry>> entryProvider, String type) {
        AtomicReference<String[]> ref = new AtomicReference<>(new String[]{"", L10N.t("blockly.extension.data_list_selector.no_entry")});
        try {
            SwingUtilities.invokeAndWait(() -> {
                DataListEntry selected = DataListSelectorDialog.openSelectorDialog(mcreator, entryProvider,
                        L10N.t("dialog.selector.title"), L10N.t("dialog.selector." + type + ".message"));
                if (selected != null)
                    ref.set(new String[]{selected.getName(), selected.getReadableName()});
            });
        } catch (Exception e) {
            LOG.error("Erro ao abrir DataListSelector: {}", e.getMessage(), e);
        }
        return ref.get();
    }

    /** Abre seletor genérico de Strings. */
    private String[] openStringEntrySelector(Function<Workspace, String[]> entryProvider, String type) {
        AtomicReference<String[]> ref = new AtomicReference<>(new String[]{"", L10N.t("blockly.extension.data_list_selector.no_entry")});
        try {
            SwingUtilities.invokeAndWait(() -> {
                String selected = StringSelectorDialog.openSelectorDialog(mcreator, entryProvider,
                        L10N.t("dialog.selector.title"), L10N.t("dialog.selector." + type + ".message"));
                if (selected != null)
                    ref.set(new String[]{selected, selected});
            });
        } catch (Exception e) {
            LOG.error("Erro ao abrir StringSelectorDialog: {}", e.getMessage(), e);
        }
        return ref.get();
    }

    /** Carrega dados de propriedades de entidades customizadas. */
    public static List<String> loadEntityDataListFromCustomEntity(Workspace workspace, String entityName, Class<? extends PropertyData<?>> type) {
        if (entityName != null) {
            var element = workspace.getModElementByName(entityName.replace("CUSTOM:", ""));
            if (element != null) {
                Object ent = element.getGeneratableElement();
                if (ent instanceof LivingEntity living) {
                    return living.entityDataEntries.stream()
                            .filter(e -> e.property().getClass().equals(type))
                            .map(e -> e.property().getName())
                            .toList();
                } else if (ent instanceof AnimatedEntity animated) {
                    return animated.entityDataEntries.stream()
                            .filter(e -> e.property().getClass().equals(type))
                            .map(e -> e.property().getName())
                            .toList();
                }
            }
        }
        return new ArrayList<>();
    }

    /** Abre seletor de entrada (entidade, gui, som, etc.). */
    @SuppressWarnings("unused")
    public void openEntrySelector(String type, String typeFilter, String customEntryProviders, JSObject callback) {
        String[] retval;
        switch (type) {
            case "entity" -> retval = openDataListEntrySelector(w ->
                    ElementUtil.loadAllEntities(w).stream().filter(e -> e.isSupportedInWorkspace(w)).toList(), "entity");
            case "spawnableEntity" -> retval = openDataListEntrySelector(w ->
                    ElementUtil.loadAllSpawnableEntities(w).stream().filter(e -> e.isSupportedInWorkspace(w)).toList(), "entity");
            case "customEntity" -> retval = openDataListEntrySelector(ElementUtil::loadCustomEntities, "entity");
            case "entitydata_logic" -> retval = openStringEntrySelector(
                    w -> loadEntityDataListFromCustomEntity(w, customEntryProviders, PropertyData.LogicType.class).toArray(String[]::new), "entity_data");
            case "entitydata_integer" -> retval = openStringEntrySelector(
                    w -> loadEntityDataListFromCustomEntity(w, customEntryProviders, PropertyData.IntegerType.class).toArray(String[]::new), "entity_data");
            case "entitydata_string" -> retval = openStringEntrySelector(
                    w -> loadEntityDataListFromCustomEntity(w, customEntryProviders, PropertyData.StringType.class).toArray(String[]::new), "entity_data");
            case "gui" -> retval = openStringEntrySelector(w -> ElementUtil.loadBasicGUIs(w).toArray(String[]::new), "gui");
            case "biome" -> retval = openDataListEntrySelector(
                    w -> ElementUtil.loadAllBiomes(w).stream().filter(e -> e.isSupportedInWorkspace(w)).toList(), "biome");
            case "global_triggers" -> {
                retval = openDataListEntrySelector(w -> {
                    List<DataListEntry> list = new ArrayList<>();
                    for (Map.Entry<String, String> entry : ext_triggers.entrySet()) {
                        DataListEntry.Dummy dummy = new DataListEntry.Dummy(entry.getKey());
                        dummy.setReadableName(entry.getValue());
                        list.add(dummy);
                    }
                    return list;
                }, "global_trigger");
                if (retval[0].isEmpty())
                    retval = new String[]{"no_ext_trigger", L10N.t("trigger.no_ext_trigger")};
            }
            default -> {
                if (type.startsWith("procedure_retval_")) {
                    VariableType variableType = VariableTypeLoader.INSTANCE.fromName(
                            StringUtils.removeStart(type, "procedure_retval_"));
                    retval = openStringEntrySelector(w -> ElementUtil.getProceduresOfType(w, variableType), "procedure");
                } 
                else if (!DataListLoader.loadDataList(type).isEmpty()) {
                    // ✅ Compatível com MCreator 2025.3
                    retval = openDataListEntrySelector(
                            w -> ElementUtil.loadDataListAndElements(
                                    w,
                                    type,
                                    typeFilter,
                                    StringUtils.split(customEntryProviders, ',')
                            ),
                            type
                    );
                } 
                else {
                    retval = new String[]{"", L10N.t("blockly.extension.data_list_selector.no_entry")};
                }
            }
        }
        callback.call("callback", retval[0], retval[1]);
    }

    /** Lista de triggers externos registrados. */
    private final Map<String, String> ext_triggers = new LinkedHashMap<>() {{
        put("no_ext_trigger", L10N.t("trigger.no_ext_trigger"));
    }};

    void addExternalTrigger(ExternalTrigger external_trigger) {
        ext_triggers.put(external_trigger.getID(), external_trigger.getName());
    }

    /** Retorna dependências de um procedimento. */
    @SuppressWarnings("unused")
    public Dependency[] getDependencies(String procedureName) {
        ModElement me = mcreator.getWorkspace().getModElementByName(procedureName);
        return me != null && me.getGeneratableElement() instanceof Procedure procedure
                ? procedure.getDependencies().toArray(Dependency[]::new)
                : new Dependency[0];
    }

    /** Tradução. */
    @SuppressWarnings("unused")
    public String t(String key) {
        return L10N.t(key);
    }

    /** Verifica se é uma variável do jogador. */
    @SuppressWarnings("unused")
    public boolean isPlayerVariable(String field) {
        return BlocklyVariables.isPlayerVariableForWorkspace(mcreator.getWorkspace(), field);
    }

    /** Retorna o nome legível de uma entrada. */
    @SuppressWarnings("unused")
    public String getReadableNameOf(String value, String type) {
        if (value.startsWith("CUSTOM:"))
            return value.substring(7);

        String datalist;
        switch (type) {
            case "entity", "spawnableEntity" -> datalist = "entities";
            case "biome" -> datalist = "biomes";
            case "arrowProjectile", "projectiles" -> datalist = "projectiles";
            case "global_triggers" -> {
                return ext_triggers.get(value);
            }
            default -> {
                return "";
            }
        }
        return DataListLoader.loadDataMap(datalist).containsKey(value)
                ? DataListLoader.loadDataMap(datalist).get(value).getReadableName()
                : "";
    }
}
