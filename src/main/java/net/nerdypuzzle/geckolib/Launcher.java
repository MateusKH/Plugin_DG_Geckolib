package net.nerdypuzzle.geckolib;

import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.PreGeneratorsLoadingEvent;
import net.mcreator.plugin.events.ui.ModElementGUIEvent;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import net.nerdypuzzle.geckolib.registry.PluginActions;
import net.nerdypuzzle.geckolib.registry.PluginElementTypes;
import net.nerdypuzzle.geckolib.registry.PluginEventTriggers;

/**
 * Classe principal do plugin GeckoLib para MCreator.
 * Compatível com Java 17 e sem dependência externa de Log4j.
 */
public class Launcher extends JavaPlugin {

    /** Sistema de log interno do plugin (sem Log4j). */
    public static void log(String message) {
        System.out.println("[GeckoLib Plugin] " + message);
    }

    /** Registro de ações do plugin. */
    public static PluginActions ACTION_REGISTRY;

    /** Instâncias ativas do plugin. */
    public static final Set<Plugin> PLUGIN_INSTANCE = new HashSet<>();

    /** Construtor do plugin. */
    public Launcher(Plugin plugin) {
        super(plugin);

        // Adiciona o plugin à lista de instâncias
        PLUGIN_INSTANCE.add(plugin);

        // Carrega os tipos de elementos personalizados antes dos geradores
        addListener(PreGeneratorsLoadingEvent.class, event -> PluginElementTypes.load());

        // Eventos GUI antes do carregamento
        addListener(ModElementGUIEvent.BeforeLoading.class, event -> SwingUtilities.invokeLater(() -> {
            PluginEventTriggers.dependencyWarning(event.getMCreator(), event.getModElementGUI());
            PluginEventTriggers.interceptProcedurePanel(event.getMCreator(), event.getModElementGUI());
        }));

        // Após o carregamento do MCreator
        addListener(MCreatorLoadedEvent.class, event -> {
            ACTION_REGISTRY = new PluginActions(event.getMCreator());
            SwingUtilities.invokeLater(() -> PluginEventTriggers.modifyMenus(event.getMCreator()));
        });

        // Mensagem de log
        log("Plugin was loaded successfully.");
    }
}