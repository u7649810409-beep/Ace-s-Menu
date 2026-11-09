package com.example.pinkmenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public class PinkMenuClient implements ClientModInitializer {

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("pinkmenu", "main"));

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        Config.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pinkmenu.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MenuScreen());
                }
            }
        });
    }

    // ---------------- Einstellungen (werden gespeichert) ----------------
    static class Config {
        private static final Properties props = new Properties();

        private static Path file() {
            return FabricLoader.getInstance().getConfigDir().resolve("pinkmenu.properties");
        }

        static boolean getBool(String key) {
            return Boolean.parseBoolean(props.getProperty(key, "false"));
        }

        static void setBool(String key, boolean value) {
            props.setProperty(key, String.valueOf(value));
            save();
        }

        static int getInt(String key) {
            try {
                return Integer.parseInt(props.getProperty(key, "50"));
            } catch (NumberFormatException e) {
                return 50;
            }
        }

        static void setInt(String key, int value) {
            props.setProperty(key, String.valueOf(value));
            save();
        }

        static void load() {
            Path f = file();
            if (!Files.exists(f)) return;
            try (InputStream in = Files.newInputStream(f)) {
                props.load(in);
            } catch (IOException ignored) {
            }
        }

        static void save() {
            try (OutputStream out = Files.newOutputStream(file())) {
                props.store(out, "Pink Menu");
            } catch (IOException ignored) {
            }
        }
    }

    // ---------------- Das Menue ----------------
    static class MenuScreen extends Screen {

        private static final String[] TABS = {"PVP", "Effects", "Skins"};
        private int tab = 0;

        MenuScreen() {
            super(Text.literal("Pink Menu"));
        }

        @Override
        protected void init() {
            int x = this.width / 2 - 100;
            int y = this.height / 2 - 55;
            String prefix = TABS[tab].toLowerCase(Locale.ROOT);

            // Inhalt des aktiven Reiters
            for (int i = 1; i <= 3; i++) {
                addDrawableChild(toggle("Funktion " + i, x, y + (i - 1) * 24, prefix + ".f" + i));
            }
            addDrawableChild(new StrengthSlider(x, y + 72, 200, 20, prefix + ".strength"));
            addDrawableChild(ButtonWidget.builder(Text.literal("Schließen"), b -> this.close())
                    .dimensions(x, y + 100, 200, 20)
                    .build());

            // Reiter rechts am Panel-Rand
            int tabX = this.width / 2 + 120 + 4;
            int tabY = this.height / 2 - 90 + 10;
            for (int t = 0; t < TABS.length; t++) {
                final int index = t;
                addDrawableChild(ButtonWidget.builder(tabLabel(t), b -> {
                    this.tab = index;
                    this.clearAndInit();
                }).dimensions(tabX, tabY + t * 26, 62, 22).build());
            }
        }

        private Text tabLabel(int index) {
            if (index == tab) {
                return Text.literal(TABS[index]).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
            }
            return Text.literal(TABS[index]).formatted(Formatting.WHITE);
        }

        private ButtonWidget toggle(String name, int x, int y, String key) {
            return ButtonWidget.builder(label(name, Config.getBool(key)), b -> {
                boolean now = !Config.getBool(key);
                Config.setBool(key, now);
                b.setMessage(label(name, now));
            }).dimensions(x, y, 200, 20).build();
        }

        private static Text label(String name, boolean on) {
            return Text.literal(name + ": " + (on ? "AN" : "AUS"));
        }

        // Ueberschreibt den Standard-Hintergrund, damit nur unser Verlauf zu sehen ist.
        public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            int w = this.width;
            int h = this.height;

            // Verlauf: dunkles Indigo -> Lila -> leuchtendes Pink
            context.fillGradient(0, 0, w, h * 35 / 100, 0xFF14083F, 0xFF5B1A9E);
            context.fillGradient(0, h * 35 / 100, w, h * 65 / 100, 0xFF5B1A9E, 0xFFD43FD0);
            context.fillGradient(0, h * 65 / 100, w, h, 0xFFD43FD0, 0xFFA81E6C);

            // Panel mit pinkem Rand
            int px1 = w / 2 - 120;
            int px2 = w / 2 + 120;
            int py1 = h / 2 - 90;
            int py2 = h / 2 + 80;
            context.fill(px1 - 1, py1 - 1, px2 + 1, py2 + 1, 0xFFFF6FE0);
            context.fill(px1, py1, px2, py2, 0xCC140632);

            // Balken, der den aktiven Reiter mit dem Panel verbindet
            int barY = py1 + 10 + tab * 26;
            context.fill(px2, barY, px2 + 4, barY + 22, 0xFFFF6FE0);

            // Titel und Name des aktiven Reiters
            int titleX = w / 2 - this.textRenderer.getWidth(this.title) / 2;
            context.drawTextWithShadow(this.textRenderer, this.title, titleX, py1 + 8, 0xFFFFC8F4);
            Text sub = Text.literal(TABS[tab]);
            int subX = w / 2 - this.textRenderer.getWidth(sub) / 2;
            context.drawTextWithShadow(this.textRenderer, sub, subX, py1 + 21, 0xFFFF8FE8);

            super.render(context, mouseX, mouseY, delta);
        }
    }

    // ---------------- Regler ----------------
    static class StrengthSlider extends SliderWidget {

        private final String key;

        StrengthSlider(int x, int y, int width, int height, String key) {
            super(x, y, width, height, Text.empty(), Config.getInt(key) / 100.0);
            this.key = key;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Stärke: " + (int) Math.round(this.value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            Config.setInt(key, (int) Math.round(this.value * 100));
        }
    }
}
