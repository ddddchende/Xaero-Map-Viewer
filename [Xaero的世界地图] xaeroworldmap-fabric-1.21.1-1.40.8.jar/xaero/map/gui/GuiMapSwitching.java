package xaero.map.gui;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.class_1074;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_410;
import net.minecraft.class_4185;
import net.minecraft.class_4588;
import net.minecraft.class_5321;
import net.minecraft.class_4597.class_4598;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget.Builder;
import xaero.lib.common.util.KeySortableByOther;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.world.MapConnectionManager;
import xaero.map.world.MapConnectionNode;
import xaero.map.world.MapDimension;

public class GuiMapSwitching {
   private static final class_2561 CONNECT_MAP = class_2561.method_43471("gui.xaero_connect_map");
   private static final class_2561 DISCONNECT_MAP = class_2561.method_43471("gui.xaero_disconnect_map");
   private MapProcessor mapProcessor;
   private MapDimension settingsDimension;
   private String[] mwDropdownValues;
   private DropDownWidget createdDimensionDropdown;
   private DropDownWidget createdMapDropdown;
   private class_4185 switchingButton;
   private class_4185 multiworldTypeOptionButton;
   private class_4185 renameButton;
   private class_4185 connectButton;
   private class_4185 deleteButton;
   private class_4185 confirmButton;
   private Tooltip serverSelectionModeBox = new Tooltip("gui.xaero_mw_server_box");
   private Tooltip mapSelectionBox = new Tooltip("gui.xaero_map_selection_box");
   public boolean active;
   private boolean writableOnInit;
   private boolean uiPausedOnUpdate;
   private boolean mapSwitchingAllowed;

   public GuiMapSwitching(MapProcessor mapProcessor) {
      this.mapProcessor = mapProcessor;
      this.mapSelectionBox.setStartWidth(200);
      this.serverSelectionModeBox.setStartWidth(200);
   }

   public void init(GuiMap mapScreen, class_310 minecraft, int width, int height) {
      boolean dimensionDDWasOpen = this.createdDimensionDropdown != null && !this.createdDimensionDropdown.isClosed();
      boolean mapDDWasOpen = this.createdMapDropdown != null && !this.createdMapDropdown.isClosed();
      this.createdDimensionDropdown = null;
      this.createdMapDropdown = null;
      this.switchingButton = null;
      this.multiworldTypeOptionButton = null;
      this.renameButton = null;
      this.deleteButton = null;
      this.confirmButton = null;
      this.settingsDimension = this.mapProcessor.getMapWorld().getFutureDimension();
      this.mapSwitchingAllowed = this.settingsDimension != null;
      synchronized(this.mapProcessor.uiPauseSync) {
         this.uiPausedOnUpdate = this.isUIPaused();
         mapScreen.addButton(this.switchingButton = new GuiMapSwitchingButton(this.active, 0, height - 20, (b) -> {
            synchronized(this.mapProcessor.uiPauseSync) {
               if (this.canToggleThisScreen()) {
                  this.active = !this.active;
                  mapScreen.method_25423(minecraft, width, height);
                  mapScreen.method_25395(this.switchingButton);
               }
            }
         }));
         if (this.mapSwitchingAllowed) {
            this.writableOnInit = this.settingsDimension.futureMultiworldWritable;
            if (this.active) {
               this.createdDimensionDropdown = this.createDimensionDropdown(this.uiPausedOnUpdate, width, mapScreen, minecraft);
               this.createdMapDropdown = this.createMapDropdown(this.uiPausedOnUpdate, width, mapScreen, minecraft);
               mapScreen.method_25429(this.createdDimensionDropdown);
               mapScreen.method_25429(this.createdMapDropdown);
               if (dimensionDDWasOpen) {
                  this.createdDimensionDropdown.setClosed(false);
               }

               if (mapDDWasOpen) {
                  this.createdMapDropdown.setClosed(false);
               }

               mapScreen.addButton(this.multiworldTypeOptionButton = new TooltipButton(width / 2 - 90, 24, 180, 20, class_2561.method_43470(this.getMultiworldTypeButtonMessage()), (b) -> {
                  synchronized(this.mapProcessor.uiPauseSync) {
                     if (this.isMapSelectionOptionEnabled()) {
                        this.mapProcessor.toggleMultiworldType(this.settingsDimension);
                        b.method_25355(class_2561.method_43470(this.getMultiworldTypeButtonMessage()));
                     }

                  }
               }, this.settingsDimension.isFutureMultiworldServerBased() ? () -> {
                  return this.serverSelectionModeBox;
               } : () -> {
                  return this.mapSelectionBox;
               }));
               mapScreen.addButton(this.renameButton = class_4185.method_46430(class_2561.method_43471("gui.xaero_rename"), (b) -> {
                  synchronized(this.mapProcessor.uiPauseSync) {
                     if (this.canRenameMap()) {
                        String currentMultiworld = this.settingsDimension.getFutureMultiworldUnsynced();
                        if (currentMultiworld != null) {
                           minecraft.method_1507(new GuiMapName(this.mapProcessor, mapScreen, mapScreen, this.settingsDimension, currentMultiworld));
                        }
                     }
                  }
               }).method_46434(width / 2 + 109, 80, 60, 20).method_46431());
               mapScreen.addButton(this.connectButton = class_4185.method_46430(this.getConnectButtonLabel(), (b) -> {
                  if (this.canConnectMap()) {
                     MapConnectionNode playerMapKey = this.settingsDimension.getMapWorld().getPlayerMapKey();
                     if (playerMapKey != null) {
                        MapConnectionNode destinationMapKey = this.settingsDimension.getSelectedMapKeyUnsynced();
                        if (destinationMapKey != null) {
                           String autoMapName = playerMapKey.getNamedString(this.settingsDimension.getMapWorld());
                           String selectedMapName = destinationMapKey.getNamedString(this.settingsDimension.getMapWorld());
                           String connectionDisplayString = autoMapName + "   §e<=>§r   " + selectedMapName;
                           MapConnectionManager mapConnections = this.settingsDimension.getMapWorld().getMapConnections();
                           boolean connected = mapConnections.isConnected(playerMapKey, destinationMapKey);
                           BooleanConsumer confirmationConsumer = (result) -> {
                              if (result) {
                                 synchronized(this.mapProcessor.uiSync) {
                                    if (connected) {
                                       mapConnections.removeConnection(playerMapKey, destinationMapKey);
                                    } else {
                                       mapConnections.addConnection(playerMapKey, destinationMapKey);
                                    }

                                    b.method_25355(this.getConnectButtonLabel());
                                    this.settingsDimension.getMapWorld().saveConfig();
                                 }
                              }

                              minecraft.method_1507(mapScreen);
                           };
                           if (connected) {
                              minecraft.method_1507(new class_410(confirmationConsumer, class_2561.method_43471("gui.xaero_wm_disconnect_from_auto_msg"), class_2561.method_43470(connectionDisplayString)));
                           } else {
                              minecraft.method_1507(new class_410(confirmationConsumer, class_2561.method_43471("gui.xaero_wm_connect_with_auto_msg"), class_2561.method_43470(connectionDisplayString)));
                           }

                        }
                     }
                  }
               }).method_46434(width / 2 + 109, 102, 60, 20).method_46431());
               mapScreen.addButton(this.deleteButton = class_4185.method_46430(class_2561.method_43471("gui.xaero_delete"), (b) -> {
                  synchronized(this.mapProcessor.uiPauseSync) {
                     if (this.canDeleteMap()) {
                        String selectedMWId = this.settingsDimension.getFutureCustomSelectedMultiworld();
                        minecraft.method_1507(new class_410((result) -> {
                           if (result) {
                              String var10000 = class_1074.method_4662("gui.xaero_delete_map_msg4", new Object[0]);
                              String mapNameAndIdLine = var10000 + ": " + this.settingsDimension.getMultiworldName(selectedMWId) + " (" + selectedMWId + ")";
                              minecraft.method_1507(new class_410((result2) -> {
                                 if (result2) {
                                    synchronized(this.mapProcessor.uiSync) {
                                       if (this.mapProcessor.getMapWorld() == this.settingsDimension.getMapWorld()) {
                                          MapDimension currentDimension = !this.mapProcessor.isMapWorldUsable() ? null : this.mapProcessor.getMapWorld().getCurrentDimension();
                                          if (this.settingsDimension == currentDimension && this.settingsDimension.getCurrentMultiworld().equals(selectedMWId)) {
                                             if (WorldMapClientConfigUtils.getDebug()) {
                                                WorldMap.LOGGER.info("Delayed map deletion!");
                                             }

                                             this.mapProcessor.requestCurrentMapDeletion();
                                          } else {
                                             if (WorldMapClientConfigUtils.getDebug()) {
                                                WorldMap.LOGGER.info("Instant map deletion!");
                                             }

                                             this.settingsDimension.deleteMultiworldMapDataUnsynced(selectedMWId);
                                          }

                                          this.settingsDimension.deleteMultiworldId(selectedMWId);
                                          this.settingsDimension.pickDefaultCustomMultiworldUnsynced();
                                          this.settingsDimension.saveConfigUnsynced();
                                          this.settingsDimension.futureMultiworldWritable = false;
                                       }
                                    }
                                 }

                                 minecraft.method_1507(mapScreen);
                              }, class_2561.method_43471("gui.xaero_delete_map_msg3"), class_2561.method_43470(mapNameAndIdLine)));
                           } else {
                              minecraft.method_1507(mapScreen);
                           }

                        }, class_2561.method_43471("gui.xaero_delete_map_msg1"), class_2561.method_43471("gui.xaero_delete_map_msg2")));
                     }
                  }
               }).method_46434(width / 2 - 168, 80, 60, 20).method_46431());
               mapScreen.addButton(this.confirmButton = class_4185.method_46430(class_2561.method_43471("gui.xaero_confirm"), (b) -> {
                  synchronized(this.mapProcessor.uiPauseSync) {
                     if (this.canConfirm()) {
                        this.confirm(mapScreen, minecraft, width, height);
                     }
                  }
               }).method_46434(width / 2 - 50, 104, 100, 20).method_46431());
               this.updateButtons(mapScreen, width, minecraft);
            } else {
               this.switchingButton.field_22763 = this.canToggleThisScreen();
            }
         } else {
            this.switchingButton.field_22763 = false;
         }

      }
   }

   public static GuiDimensionOptions getSortedDimensionOptions(MapDimension dim) {
      int selected = false;
      class_5321<class_1937> currentDim = dim.getDimId();
      List<KeySortableByOther<class_5321<class_1937>>> sortableList = new ArrayList();
      Iterator var4 = dim.getMapWorld().getDimensionsList().iterator();

      while(var4.hasNext()) {
         MapDimension dimension = (MapDimension)var4.next();
         sortableList.add(new KeySortableByOther(dimension.getDimId(), new Comparable[]{dimension.getDimId().method_29177().toString()}));
      }

      Collections.sort(sortableList);
      int selected = getDropdownSelectionIdFromValue(sortableList, currentDim);
      class_5321<class_1937>[] values = new class_5321[0];
      values = (class_5321[])((ArrayList)sortableList.stream().map(KeySortableByOther::getKey).collect(ArrayList::new, ArrayList::add, ArrayList::addAll)).toArray(values);
      return new GuiDimensionOptions(selected, values);
   }

   private DropDownWidget createDimensionDropdown(boolean paused, int width, GuiMap mapScreen, class_310 minecraft) {
      GuiDimensionOptions dimOptions = getSortedDimensionOptions(this.settingsDimension);
      List<String> dropdownLabels = new ArrayList();
      class_5321<class_1937> currentWorldDim = this.mapProcessor.getWorld() == null ? null : this.mapProcessor.getWorld().method_27983();
      class_5321[] finalValues = dimOptions.values;
      int var9 = finalValues.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         class_5321<class_1937> k = finalValues[var10];
         String result = k.method_29177().toString();
         if (result.startsWith("minecraft:")) {
            result = result.substring(10);
         }

         if (k == currentWorldDim) {
            result = result + " (auto)";
         }

         dropdownLabels.add(result);
      }

      finalValues = dimOptions.values;
      DropDownWidget result = Builder.begin().setOptions((String[])dropdownLabels.toArray(new String[0])).setX(width / 2 - 100).setY(64).setW(200).setSelected(dimOptions.selected).setCallback((dd, i) -> {
         class_5321<class_1937> selectedValue = finalValues[i];
         this.settingsDimension = this.settingsDimension.getMapWorld().getDimension(selectedValue);
         if (selectedValue == currentWorldDim) {
            selectedValue = null;
         }

         this.settingsDimension.getMapWorld().setCustomDimensionId(selectedValue);
         this.mapProcessor.checkForWorldUpdate();
         DropDownWidget newDropDown = this.createMapDropdown(this.uiPausedOnUpdate, width, mapScreen, minecraft);
         mapScreen.replaceWidget(this.createdMapDropdown, newDropDown);
         this.createdMapDropdown = newDropDown;
         this.updateButtons(mapScreen, width, minecraft);
         return true;
      }).setContainer(mapScreen).setNarrationTitle(class_2561.method_43471("gui_xaero_wm_dropdown_dimension_select")).build();
      return result;
   }

   private DropDownWidget createMapDropdown(boolean paused, int width, GuiMap mapScreen, class_310 minecraft) {
      int selected = 0;
      Object mwDropdownNames;
      if (!paused) {
         String currentMultiworld = this.settingsDimension.getFutureMultiworldUnsynced();
         List<KeySortableByOther<String>> sortableList = new ArrayList();
         Iterator var9 = this.settingsDimension.getMultiworldIdsCopy().iterator();

         while(var9.hasNext()) {
            String mwId = (String)var9.next();
            sortableList.add(new KeySortableByOther(mwId, new Comparable[]{this.settingsDimension.getMultiworldName(mwId).toLowerCase()}));
         }

         if (currentMultiworld != null) {
            int currentIndex = getDropdownSelectionIdFromValue(sortableList, currentMultiworld);
            if (currentIndex == -1) {
               sortableList.add(new KeySortableByOther(currentMultiworld, new Comparable[]{this.settingsDimension.getMultiworldName(currentMultiworld).toLowerCase()}));
            }
         }

         Collections.sort(sortableList);
         if (currentMultiworld != null) {
            selected = getDropdownSelectionIdFromValue(sortableList, currentMultiworld);
         }

         this.mwDropdownValues = (String[])((ArrayList)sortableList.stream().map(KeySortableByOther::getKey).collect(ArrayList::new, ArrayList::add, ArrayList::addAll)).toArray(new String[0]);
         Stream var10000 = sortableList.stream().map(KeySortableByOther::getKey);
         MapDimension var10001 = this.settingsDimension;
         Objects.requireNonNull(var10001);
         mwDropdownNames = (List)var10000.map(var10001::getMultiworldName).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
         ((List)mwDropdownNames).add("§8" + class_1074.method_4662("gui.xaero_create_new_map", new Object[0]));
      } else {
         mwDropdownNames = new ArrayList();
         this.mwDropdownValues = null;
         ((List)mwDropdownNames).add("§7" + class_1074.method_4662("gui.xaero_map_menu_please_wait", new Object[0]));
      }

      DropDownWidget result = Builder.begin().setOptions((String[])((List)mwDropdownNames).toArray(new String[0])).setX(width / 2 - 100).setY(84).setW(200).setSelected(selected).setCallback((dd, i) -> {
         synchronized(this.mapProcessor.uiPauseSync) {
            if (!this.isUIPaused() && !this.uiPausedOnUpdate) {
               if (i < this.mwDropdownValues.length) {
                  this.mapProcessor.setMultiworld(this.settingsDimension, this.mwDropdownValues[i]);
                  this.updateButtons(mapScreen, width, minecraft);
                  return true;
               } else {
                  minecraft.method_1507(new GuiMapName(this.mapProcessor, mapScreen, mapScreen, this.settingsDimension, (String)null));
                  return false;
               }
            } else {
               return false;
            }
         }
      }).setContainer(mapScreen).setNarrationTitle(class_2561.method_43471("gui_xaero_wm_dropdown_map_select")).build();
      result.setActive(!paused);
      return result;
   }

   private boolean isUIPaused() {
      return this.mapProcessor.isUIPaused() || this.mapProcessor.isWaitingForWorldUpdate();
   }

   private boolean isMapSelectionOptionEnabled() {
      return !this.isUIPaused() && !this.settingsDimension.isFutureMultiworldServerBased() && this.settingsDimension.getMapWorld().isMultiplayer();
   }

   private boolean canToggleThisScreen() {
      return !this.isUIPaused() && this.settingsDimension != null && this.settingsDimension.futureMultiworldWritable;
   }

   private boolean canDeleteMap() {
      return !this.isUIPaused() && !this.settingsDimension.isFutureUsingWorldSaveUnsynced() && this.mwDropdownValues != null && this.mwDropdownValues.length > 1 && this.settingsDimension.getFutureCustomSelectedMultiworld() != null;
   }

   private boolean canRenameMap() {
      return !this.isUIPaused() && !this.settingsDimension.isFutureUsingWorldSaveUnsynced();
   }

   private boolean canConnectMap() {
      if (!this.mapProcessor.getMapWorld().isMultiplayer()) {
         return false;
      } else {
         MapConnectionNode playerMapKey = this.settingsDimension.getMapWorld().getPlayerMapKey();
         if (playerMapKey == null) {
            return false;
         } else {
            MapConnectionNode destinationMapKey = this.settingsDimension.getSelectedMapKeyUnsynced();
            if (destinationMapKey == null) {
               return false;
            } else {
               return !destinationMapKey.equals(playerMapKey);
            }
         }
      }
   }

   private boolean canConfirm() {
      return !this.isUIPaused();
   }

   private class_2561 getConnectButtonLabel() {
      synchronized(this.mapProcessor.uiPauseSync) {
         if (this.isUIPaused()) {
            return CONNECT_MAP;
         } else {
            MapConnectionNode playerMapKey = this.settingsDimension.getMapWorld().getPlayerMapKey();
            if (playerMapKey == null) {
               return CONNECT_MAP;
            } else {
               MapConnectionNode destinationMapKey = this.settingsDimension.getSelectedMapKeyUnsynced();
               if (destinationMapKey == null) {
                  return CONNECT_MAP;
               } else {
                  MapConnectionManager mapConnections = this.settingsDimension.getMapWorld().getMapConnections();
                  return mapConnections.isConnected(playerMapKey, destinationMapKey) ? DISCONNECT_MAP : CONNECT_MAP;
               }
            }
         }
      }
   }

   private void updateButtons(GuiMap mapScreen, int width, class_310 minecraft) {
      synchronized(this.mapProcessor.uiPauseSync) {
         boolean isPaused = this.isUIPaused();
         if (this.uiPausedOnUpdate != isPaused) {
            DropDownWidget newDropDown = !this.active ? null : this.createMapDropdown(isPaused, width, mapScreen, minecraft);
            if (newDropDown != null) {
               if (this.createdMapDropdown != null) {
                  mapScreen.replaceWidget(this.createdMapDropdown, newDropDown);
               } else {
                  mapScreen.method_25429(newDropDown);
               }
            } else if (this.createdMapDropdown != null) {
               mapScreen.method_37066(this.createdMapDropdown);
            }

            this.createdMapDropdown = !this.active ? null : newDropDown;
            this.uiPausedOnUpdate = isPaused;
         }

         this.switchingButton.field_22763 = this.canToggleThisScreen();
         if (this.deleteButton != null) {
            this.deleteButton.field_22763 = this.canDeleteMap();
         }

         if (this.renameButton != null) {
            this.renameButton.field_22763 = this.canRenameMap();
         }

         if (this.connectButton != null) {
            this.connectButton.field_22763 = this.canConnectMap();
            this.connectButton.method_25355(this.getConnectButtonLabel());
         }

         if (this.multiworldTypeOptionButton != null) {
            this.multiworldTypeOptionButton.field_22763 = this.isMapSelectionOptionEnabled();
         }

         if (this.confirmButton != null) {
            this.confirmButton.field_22763 = this.canConfirm();
         }

      }
   }

   private String getMultiworldTypeButtonMessage() {
      int multiworldType = this.settingsDimension.getMapWorld().getFutureMultiworldType(this.settingsDimension);
      String var10000 = class_1074.method_4662("gui.xaero_map_selection", new Object[0]);
      return var10000 + ": " + class_1074.method_4662(this.settingsDimension.isFutureMultiworldServerBased() ? "gui.xaero_mw_server" : (multiworldType == 0 ? "gui.xaero_mw_single" : (multiworldType == 1 ? "gui.xaero_mw_manual" : "gui.xaero_mw_spawn")), new Object[0]);
   }

   public void confirm(GuiMap mapScreen, class_310 minecraft, int width, int height) {
      if (this.mapProcessor.confirmMultiworld(this.settingsDimension)) {
         this.active = false;
         mapScreen.method_25423(minecraft, width, height);
      }

   }

   private static <S> int getDropdownSelectionIdFromValue(List<KeySortableByOther<S>> values, S value) {
      for(int selected = 0; selected < values.size(); ++selected) {
         if (((KeySortableByOther)values.get(selected)).getKey().equals(value)) {
            return selected;
         }
      }

      return -1;
   }

   public void preMapRender(GuiMap mapScreen, class_310 minecraft, int width, int height) {
      if (!this.active && this.settingsDimension != null && !this.settingsDimension.futureMultiworldWritable) {
         this.active = true;
         mapScreen.method_25423(minecraft, width, height);
      }

      if (this.mapSwitchingAllowed && (this.createdMapDropdown == null || this.createdMapDropdown.isClosed())) {
         synchronized(this.mapProcessor.uiPauseSync) {
            if (this.uiPausedOnUpdate != this.isUIPaused()) {
               this.updateButtons(mapScreen, width, minecraft);
            }
         }
      }

      if (this.active && this.settingsDimension != null && this.createdMapDropdown.isClosed() && !this.uiPausedOnUpdate) {
         String currentMultiworld = this.settingsDimension.getFutureMultiworldUnsynced();
         if (currentMultiworld != null) {
            String currentDropdownSelection = this.mwDropdownValues[this.createdMapDropdown.getSelected()];
            if (!currentMultiworld.equals(currentDropdownSelection) || this.writableOnInit != this.settingsDimension.futureMultiworldWritable) {
               mapScreen.method_25423(minecraft, width, height);
            }
         }
      }

   }

   public void renderText(class_332 guiGraphics, class_310 minecraft, int mouseX, int mouseY, int width, int height) {
      if (this.active) {
         String selectMapString = class_1074.method_4662("gui.xaero_select_map", new Object[0]) + ":";
         class_4598 renderTypeBuffers = this.mapProcessor.getCvc().getRenderTypeBuffers();
         class_4588 backgroundVertexBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_COLOR_OVERLAY);
         MapRenderHelper.drawStringWithBackground(guiGraphics, minecraft.field_1772, (String)selectMapString, width / 2 - minecraft.field_1772.method_1727(selectMapString) / 2, 49, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer);
         renderTypeBuffers.method_22993();
      }
   }

   public void postMapRender(class_332 guiGraphics, class_310 minecraft, int mouseX, int mouseY, int width, int height) {
   }
}
