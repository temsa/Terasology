/*
 * Copyright 2011 Benjamin Glatzel <benjamin.glatzel@me.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.gui.menus;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.lwjgl.opengl.Display;
import org.terasology.game.CoreRegistry;
import org.terasology.game.GameEngine;
import org.terasology.game.modes.StateSinglePlayer;
import org.terasology.logic.manager.AssetManager;
import org.terasology.logic.manager.Config;
import org.terasology.logic.manager.GUIManager;
import org.terasology.logic.manager.PathManager;
import org.terasology.logic.world.WorldUtil;
import org.terasology.rendering.gui.components.*;
import org.terasology.rendering.gui.dialogs.UIDialogCreateNewWorld;
import org.terasology.rendering.gui.framework.*;

import javax.vecmath.Vector2f;
import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Select world menu screen.
 *
 * @author Anton Kireev <adeon.k87@gmail.com>
 *
 */
public class UISelectWorldMenu extends UIDisplayWindow {
    private Logger logger = Logger.getLogger(getClass().getName());

    final UIImageOverlay _overlay;
    final UIList _list;
    final UIButton _goToBack;
    final UIButton _createNewWorld;
    final UIButton _loadFromList;
    final UIButton _deleteFromList;
    final UIDialogCreateNewWorld _window;

    public UISelectWorldMenu() {
        maximize();
        _overlay = new UIImageOverlay(AssetManager.loadTexture("engine:menuBackground"));
        _overlay.setVisible(true);

        _window = new UIDialogCreateNewWorld("Create new world", new Vector2f(512f, 256f));
        _window.center();

        _window.setModal(true);

        GUIManager.getInstance().addWindow(_window, "generate_world");

        _list = new UIList(new Vector2f(512f, 256f));
        _list.setVisible(true);

        _list.addDoubleClickListener(new IClickListener() {
            public void clicked(UIDisplayElement element) {
                loadSelectedWorld();
            }
        });


        _goToBack = new UIButton(new Vector2f(256f, 32f));
        _goToBack.getLabel().setText("Go to back");
        _goToBack.setVisible(true);

        _loadFromList = new UIButton(new Vector2f(128f, 32f));
        _loadFromList.getLabel().setText("Load");
        _loadFromList.setVisible(true);

        _createNewWorld = new UIButton(new Vector2f(192f, 32f));
        _createNewWorld.getLabel().setText("Create new world");
        _createNewWorld.setVisible(true);

        _deleteFromList = new UIButton(new Vector2f(128f, 32f));
        _deleteFromList.getLabel().setText("Delete");
        _deleteFromList.setVisible(true);

        _createNewWorld.addClickListener(new IClickListener() {
            public void clicked(UIDisplayElement element) {
                GUIManager.getInstance().setFocusedWindow(_window);
                _window.clearInputControls();
                UIInput inputWorldName = (UIInput)_window.getElementById("inputWorldTitle");
                inputWorldName.setValue(_window.getWorldName());

            }
        });

        _deleteFromList.addClickListener(new IClickListener() {
            public void clicked(UIDisplayElement element) {
                
                if(_list.getSelectedItem() == null){
                    GUIManager.getInstance().showMessage("Deleting error", "Please choose the world.");
                    return;
                }

                try{
                    ConfigObject  config = (ConfigObject)_list.getSelectedItem().getValue();
                    File world = PathManager.getInstance().getWorldSavePath((String) config.get("worldTitle"));
                    WorldUtil.deleteWorld(world);
                    _list.removeSelectedItem();
                }catch(Exception e){
                    GUIManager.getInstance().showMessage("Deleting error", "Failed deleting world data object. Sorry.");
                }
            }
        });

        _loadFromList.addClickListener(new IClickListener() {
            public void clicked(UIDisplayElement element) {
                loadSelectedWorld();
            }
        });

        fillList();

        addDisplayElement(_overlay);
        addDisplayElement(_list, "list");
        addDisplayElement(_loadFromList, "loadFromListButton");
        addDisplayElement(_goToBack, "goToBackButton");
        addDisplayElement(_createNewWorld, "createWorldButton");
        addDisplayElement(_deleteFromList, "deleteFromListButton");
        update();
    }

    @Override
    public void update() {
        super.update();
        _list.centerHorizontally();
        _list.getPosition().y = 230f;

        _createNewWorld.getPosition().x = _list.getPosition().x;
        _createNewWorld.getPosition().y = _list.getPosition().y  + _list.getSize().y + 32f;

        _loadFromList.getPosition().x =_createNewWorld.getPosition().x + _createNewWorld.getSize().x + 15f;
        _loadFromList.getPosition().y =_createNewWorld.getPosition().y;

        _deleteFromList.getPosition().x =_loadFromList.getPosition().x + _loadFromList.getSize().x + 15f;
        _deleteFromList.getPosition().y =_loadFromList.getPosition().y;


        _goToBack.centerHorizontally();

        _goToBack.getPosition().y = Display.getHeight() - _goToBack.getSize().y - 32f;

    }

    private void loadSelectedWorld(){

        if(_list.size()<1){
            GUIManager.getInstance().showMessage("Loading error", "You haven't worlds. Please create new.");
            return;
        }

        if(_list.getSelectedItem() == null){
            GUIManager.getInstance().showMessage("Loading error", "Please choose the world.");
            return;
        }

        try{
            ConfigObject  config = (ConfigObject)_list.getSelectedItem().getValue();
            Config.getInstance().setDefaultSeed((String)config.get("worldSeed"));
            Config.getInstance().setWorldTitle((String) config.get("worldTitle"));
            CoreRegistry.get(GameEngine.class).changeState(new StateSinglePlayer(config.get("worldTitle").toString(), config.get("worldSeed").toString()));
        }catch (Exception e){
            GUIManager.getInstance().showMessage("Loading error", "Failed reading world data object. Sorry.");
        }
    }

    public void fillList(){
        _list.removeAll();

        ConfigObject config = null;
        File worldCatalog = PathManager.getInstance().getWorldPath();

        for(File file : worldCatalog.listFiles(new FileFilter() {
            public boolean accept(File file) {
                if(file.isDirectory()){
                    return true;
                }else{
                    return false;
                }
            }
        })){
            File worldManifest = new File(file, "WorldManifest.groovy");
            if (!worldManifest.exists())
                continue;
            try {
                config = new ConfigSlurper().parse(worldManifest.toURI().toURL());
                if(config.get("worldTitle")!=null&&config.get("worldSeed")!=null){
                    _list.addItem((String) config.get("worldTitle"), config);
                }
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "Failed reading world data object. Sorry.", e);
            }
        }
    }
}
