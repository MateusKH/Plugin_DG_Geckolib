package net.nerdypuzzle.geckolib.parts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import net.mcreator.generator.GeneratorUtils;
import net.mcreator.io.Transliteration;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.action.BasicAction;
import net.mcreator.ui.dialogs.file.FileDialogs;
import net.mcreator.ui.init.L10N;
import net.nerdypuzzle.geckolib.registry.PluginActions;

/**
 * Ações de importação de modelos GeckoLib e Display Settings.
 * Compatível com MCreator 2024.x e builds de plugin.
 */
public class PluginModelActions {

    /* ===================== Diretórios ===================== */

    public static File getAnimationsDir(MCreator mcreator) {
        return new File(getAssetsRoot(mcreator), "animations/");
    }

    public static File getGeometryDir(MCreator mcreator) {
        return new File(getAssetsRoot(mcreator), "geo/");
    }

    public static File getDisplaySettingsDir(MCreator mcreator) {
        return new File(getAssetsRoot(mcreator), "models/displaysettings/");
    }

    private static File getAssetsRoot(MCreator mcreator) {
        return GeneratorUtils.getSpecificRoot(
                mcreator.getWorkspace(),
                mcreator.getWorkspace().getGeneratorConfiguration(),
                "mod_assets_root"
        );
    }

    /* ===================== Listagem ===================== */

    private static List<File> listFilesInDir(File dir, String extension) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return Collections.emptyList();

        File[] files = dir.listFiles((f) -> f.isFile() && f.getName().endsWith(extension));
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    public static List<File> getGeomodels(MCreator mcreator) {
        return listFilesInDir(getGeometryDir(mcreator), ".geo.json");
    }

    public static List<File> getDisplaysettings(MCreator mcreator) {
        return listFilesInDir(getDisplaySettingsDir(mcreator), ".json").stream()
                .filter(f -> !f.getName().endsWith(".geo.json") && !f.getName().endsWith(".animation.json"))
                .collect(Collectors.toList());
    }

    /* ===================== Ações ===================== */

    /** Importa um modelo GeckoLib (.geo.json) e opcionalmente o arquivo de animação. */
    public static void importGeckoLibModels(MCreator mcreator, File geoModel) {
        File geometryDir = getGeometryDir(mcreator);
        File animationDir = getAnimationsDir(mcreator);

        if (!geometryDir.exists()) geometryDir.mkdirs();
        if (!animationDir.exists()) animationDir.mkdirs();

        String baseName = normalizeName(geoModel.getName())
                .replace(".geo.", "");

        try {
            copyFileSafe(geoModel, new File(geometryDir, baseName + ".geo.json"));
        } catch (IOException e) {
            showError(mcreator, "error.import_geckolib_model", e);
            return;
        }

        File animation = FileDialogs.getOpenDialog(mcreator, new String[]{".animation.json"});
        if (animation != null) {
            try {
                copyFileSafe(animation, new File(animationDir, baseName + ".animation.json"));
            } catch (IOException e) {
                showError(mcreator, "error.import_animation", e);
            }
        }
    }

    /** Importa um Display Settings (.json). */
    public static void importDisplaySettings(MCreator mcreator, File displaySettings) {
        File dir = getDisplaySettingsDir(mcreator);
        if (!dir.exists()) dir.mkdirs();

        try {
            copyFileSafe(displaySettings, new File(dir, normalizeName(displaySettings.getName())));
        } catch (IOException e) {
            showError(mcreator, "error.import_display_settings", e);
        }
    }

    /* ===================== Utilitários ===================== */

    private static String normalizeName(String name) {
        return Transliteration.transliterateString(name)
                .toLowerCase(Locale.ENGLISH)
                .trim()
                .replace(":", "")
                .replace(" ", "_");
    }

    private static void copyFileSafe(File src, File dest) throws IOException {
        if (src == null || !src.exists())
            throw new IOException("Arquivo de origem inexistente: " + src);

        Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static void showError(MCreator mcreator, String key, Exception e) {
        JOptionPane.showMessageDialog(
                mcreator, // ✅ Usa diretamente a janela principal do MCreator
                L10N.t(key) + "\n\n" + e.getMessage(),
                L10N.t("dialog.error"),
                JOptionPane.WARNING_MESSAGE
        );
    }

    /* ===================== Classes de ação ===================== */

    public static class GECKOLIB extends BasicAction {
        public GECKOLIB(PluginActions registry) {
            super(registry,
                    L10N.t("action.workspace.resources.import_geckolib_model"),
                    e -> {
                        File geoModel = FileDialogs.getOpenDialog(registry.getMCreator(), new String[]{".geo.json"});
                        if (geoModel != null)
                            importGeckoLibModels(registry.getMCreator(), geoModel);
                    });
        }
    }

    public static class DISPLAYSETTINGS extends BasicAction {
        public DISPLAYSETTINGS(PluginActions registry) {
            super(registry,
                    L10N.t("action.workspace.resources.import_display_settings"),
                    e -> {
                        File displaySettings = FileDialogs.getOpenDialog(registry.getMCreator(), new String[]{".json"});
                        if (displaySettings != null)
                            importDisplaySettings(registry.getMCreator(), displaySettings);
                    });
        }
    }
}
