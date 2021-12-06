package com.umollu.continuebutton.mixin;

import com.umollu.continuebutton.ContinueButtonMod;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(value = TitleScreen.class, priority = 1001)
public class MixinTitleScreen extends Screen {
    private LevelSummary level = null;
    private LevelSummary localLevel = null;
    private final MultiplayerServerListPinger serverListPinger = new MultiplayerServerListPinger();
    private ServerInfo serverInfo = null;
    private boolean isFirstRender = true;

    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(at = @At("HEAD"), method = "initWidgetsNormal(II)V")
    public void drawMenuButton(int y, int spacingY, CallbackInfo info) {

        ButtonWidget continueButton = new ButtonWidget(this.width / 2 - 100, y, 98, 20, new TranslatableText("continuebutton.continueButtonTitle"), button -> {
            if(ContinueButtonMod.lastLocal) {
                LevelStorage levelStorage = this.client.getLevelStorage();
                List<LevelSummary> levels = null;
                try {
                    levels = levelStorage.getLevelList();
                } catch (LevelStorageException e) {
                    e.printStackTrace();
                }
                if (levels.isEmpty()) {
                    this.client.setScreen(CreateWorldScreen.create((Screen) null));
                } else {
                    Collections.sort(levels);
                    level = levels.get(0);

                    if (!level.isLocked()) {
                        if (level.isFutureLevel()) {
                            this.client.setScreen(new ConfirmScreen((bl) -> {
                                if (bl) {
                                    try {
                                        start();
                                    } catch (Exception var3) {
                                        this.client.setScreen(new NoticeScreen(() -> {
                                            this.client.setScreen(this);
                                        }, new TranslatableText("selectWorld.futureworld.error.title"), new TranslatableText("selectWorld.futureworld.error.text")));
                                    }
                                } else {
                                    this.client.setScreen(this);
                                }

                            }, new TranslatableText("selectWorld.versionQuestion"), new TranslatableText("selectWorld.versionWarning", new Object[]{this.level.getVersion(), new TranslatableText("selectWorld.versionJoinButton"), ScreenTexts.CANCEL})));
                        } else {
                            start();
                        }

                    }
                }
            }
            else {
                ConnectScreen.connect(this, this.client, ServerAddress.parse(serverInfo.address), serverInfo);
            }

        }, (button, matrixStack, i, j) -> {
            if(ContinueButtonMod.lastLocal) {
                if(localLevel == null) {
                    List<OrderedText> list = new ArrayList<>();
                    list.add(new TranslatableText("selectWorld.create").formatted(Formatting.GRAY).asOrderedText());
                    this.renderOrderedTooltip(matrixStack, list, i, j);
                } else {
                    List<OrderedText> list = new ArrayList<>();
                    list.add(new TranslatableText("menu.singleplayer").formatted(Formatting.GRAY).asOrderedText());
                    list.add(new LiteralText(localLevel.getDisplayName()).asOrderedText());
                    this.renderOrderedTooltip(matrixStack, list, i, j);
                }
            } else if (serverInfo!= null) {

                List<OrderedText> list = new ArrayList<>(this.client.textRenderer.wrapLines(serverInfo.label, 270));
                list.add(0, new LiteralText(serverInfo.name).formatted(Formatting.GRAY).asOrderedText());
                this.renderOrderedTooltip(matrixStack, list, i, j);
            }
        });
        Screens.getButtons(this).add(continueButton);
    }

    private void start() {
        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (this.client.getLevelStorage().levelExists(this.level.getName())) {
            this.client.setScreenAndRender(new SaveLevelScreen(new TranslatableText("selectWorld.data_read")));
            this.client.startIntegratedServer(this.level.getName());
        }
    }

    @Inject(at = @At("HEAD"), method = "init()V")
    public void initAtHead(CallbackInfo info) {
        isFirstRender = true;
    }

    @Inject(at = @At("TAIL"), method = "init()V")
    public void init(CallbackInfo info) {
        for (ClickableWidget button : Screens.getButtons(this)) {
            if(button.visible && !button.getMessage().equals(new TranslatableText("continuebutton.continueButtonTitle"))) {
                button.x = this.width / 2 + 2;
                button.setWidth(98);
                break;
            }
        }
    }

    private void atFirstRender() {
        new Thread(() -> {
            if(ContinueButtonMod.lastLocal) {
                LevelStorage levelStorage = this.client.getLevelStorage();
                List<LevelSummary> levels = null;
                try {
                    levels = levelStorage.getLevelList();
                } catch (LevelStorageException e) {
                    e.printStackTrace();
                }
                if (levels.isEmpty()) {
                    localLevel = null;
                } else {
                    Collections.sort(levels);
                    localLevel = levels.get(0);
                }
            } else {
                ServerList serverList = new ServerList(this.client);
                ServerInfo serverInList = null;
                for(int i = 0; i < serverList.size(); i++) {
                    if(serverList.get(i).address.equalsIgnoreCase(ContinueButtonMod.serverAddress)) {
                        serverInList = serverList.get(i);
                    }
                }

                if(serverInList == null) {
                    ContinueButtonMod.lastLocal = true;
                    ContinueButtonMod.serverName = "";
                    ContinueButtonMod.serverAddress = "";
                    ContinueButtonMod.saveConfig();
                }
                else {
                    serverInfo = serverInList;

                    ContinueButtonMod.lastLocal = false;
                    ContinueButtonMod.serverName = serverInfo.name;
                    ContinueButtonMod.serverAddress = serverInfo.address;
                    ContinueButtonMod.saveConfig();

                    serverInfo.label = new TranslatableText("multiplayer.status.pinging");
                    try {
                        serverListPinger.add(serverInfo, () -> {
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void renderAtHead(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if(isFirstRender) {
            isFirstRender = false;
            atFirstRender();
        }
    }

    @Inject(at = @At("RETURN"), method = "tick()V")
    public void tick(CallbackInfo info) {
        serverListPinger.tick();
    }
    @Inject(at = @At("RETURN"), method = "removed()V")
    public void removed(CallbackInfo info) {
        serverListPinger.cancel();
    }
}