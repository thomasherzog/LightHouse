package com.illuminai.vision.frontend;

import com.illuminai.vision.backend.math.Matrix3x3;
import com.illuminai.vision.backend.math.Vector;
import com.illuminai.vision.backend.math.Vector3d;
import com.illuminai.vision.backend.render.Intersection;
import com.illuminai.vision.backend.render.Ray;
import com.illuminai.vision.backend.render.Raytracer;
import com.illuminai.vision.backend.scene.Camera;
import com.illuminai.vision.backend.scene.Scene;
import com.illuminai.vision.backend.scene.shape.Shape;
import com.illuminai.vision.frontend.actor.Actor;
import com.illuminai.vision.frontend.listener.EventExecuter;
import com.illuminai.vision.frontend.listener.GameListener;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class Render implements EventExecuter {
    private final Screen renderOn, tempScreen;
    private ScreenAverager averager;
    private final GameCanvas parent;
    private Raytracer tracer;
    private long lastFrame;

    private Actor.Mode mode;

    private Shape selectedShape;

    private Settings settings;

    private FixRotator rot;

    private Actor actor;

    private class FixRotator {
        double angleZ;
        double angleY;
        double distance;
        FixRotator() {
            angleZ = 0;
            angleY = 0;
            distance = 1;
        }

        void apply() {
            if(selectedShape == null) {
                return;
            }
            Vector3d pos = new Vector3d(-distance,0,0);

            pos = Matrix3x3.createRotationMatrix('y', angleY).transformed(pos);
            pos = Matrix3x3.createRotationMatrix('z', angleZ).transformed(pos);

            Render.this.tracer.getCamera().setRotation(new Vector3d(0, (angleY > Math.PI) ? angleY-Math.PI : (angleY < -Math.PI ? angleY + Math.PI: angleY), angleZ));
            Render.this.tracer.getCamera().setPosition(selectedShape.getPosition().add(pos));
        }

        void addZ(double angle) {
            this.angleZ += angle;
        }

        void addY(double angle) {
            this.angleY += angle;
        }

        void factorDistance(double f) {
            distance *= f;
        }
    }

    public Render(Screen renderOn, GameCanvas parent) {
        this.renderOn = renderOn;
        this.settings = new Settings();
        initSettings();

        rot = null;
        this.mode = Actor.MODE_DEFAULT;

        this.tempScreen = new Screen(renderOn.getWidth(), renderOn.getHeight());
        this.parent = parent;
        this.parent.getListener().addExecuter(this);
        this.averager = new ScreenAverager(settings.getSamples(), renderOn.getWidth(), renderOn.getHeight());

        this.actor = new Actor(this);

        init();
    }

    private void init() {
        Scene scene = new Scene();
        scene.sceneInit();
        tracer = new Raytracer(scene);
    }

    private void initSettings() {
        settings.setPause(false);
        settings.setSamples(1);
    }

    /**
     * Applies the settings to the render engine
     */
    private void applySettings() {
        if (averager.getAmount() != settings.getSamples()) {
            averager.setAmount(settings.getSamples());
        }
    }

    public void render() {
        applySettings();
        if (!settings.getPause()) {
            lastFrame = System.currentTimeMillis();

            for (int i = 0; i < averager.getAmount(); i++) {
                averager.getScreens()[i] = tracer.renderScene(i == averager.getAmount() - 1 ? 0 : Math.random());
            }


            tempScreen.drawScreen(0, 0, averager.calculateAverage());
            lastFrame = System.currentTimeMillis() - lastFrame;
        }
        renderOn.drawScreen(0, 0, tempScreen);
        drawHUD();
    }

    private void drawHUD() {
        if (selectedShape != null) {
            long[] outline = tracer.getOutliner();
            int w = (int) tracer.getRenderWidth();
            int h = (int) tracer.getRenderHeight();
            //Do not count first and last pixel
            //Makes checking more complicated because of bounds-checking
            for (int x = 1; x < w - 1; x++) {
                for (int y = 1; y < h - 1; y++) {
                    int i = x + y * w;
                    if (outline[i] == selectedShape.getId()) {
                        if (outline[i - 1] != selectedShape.getId() || outline[i + 1] != selectedShape.getId() || outline[i + w] != selectedShape.getId() || outline[i - w] != selectedShape.getId()) {
                            renderOn.setPixel(i, 0xff00ff);
                        }
                    }
                }
            }
        }

        String text = "";
        text += "renderResolution:  " + this.tracer.getRenderWidth() + "/" + this.tracer.getRenderHeight() + "\n";
        text += "displayResolution: " + this.parent.getWidth() + "/" + this.parent.getHeight() + "\n";

        text += "selected: ";
        if (selectedShape != null) {
            text += "\n  " + selectedShape.getClass().getName();
        }
        text += "\n";

        text += "This Frame: " + lastFrame + " ms";

        Screen screen = FontCreator.createFont(text, 0x0, -1);
        renderOn.drawScreen(0, 0, screen.getScaledScreen(renderOn.getWidth() * 3 / 4, screen.getHeight() / 2));
    }

    public Screen getScreen() {
        return renderOn;
    }

    public void tick() {
        GameListener l = parent.getListener();
        Vector3d rotation = tracer.getCamera().getRotation();
        if (!settings.getPause()) {
            /*
            if (l.isKeyDown(KeyEvent.VK_W)) {
                tracer.getCamera().moveForward(.1);
            }
            if (l.isKeyDown(KeyEvent.VK_S)) {
                tracer.getCamera().moveForward(-.1);
            }
            if (l.isKeyDown(KeyEvent.VK_D)) {
                tracer.getCamera().moveSideward(-.1);
            }
            if (l.isKeyDown(KeyEvent.VK_A)) {
                tracer.getCamera().moveSideward(.1);
            }
            if (l.isKeyDown(KeyEvent.VK_SPACE)) {
                tracer.getCamera().moveUpwards(.1);
            }
            if (l.isKeyDown(KeyEvent.VK_SHIFT)) {
                tracer.getCamera().moveUpwards(-.1);
            }
            */

            if (l.isKeyDown(KeyEvent.VK_L)) {
                rotation.setZ(rotation.getZ() - .05);
            }
            if (l.isKeyDown(KeyEvent.VK_J)) {
                rotation.setZ(rotation.getZ() + .05);
            }
            if (l.isKeyDown(KeyEvent.VK_I)) {
                rotation.setY(rotation.getY() + .05);
            }
            if (l.isKeyDown(KeyEvent.VK_K)) {
                rotation.setY(rotation.getY() - .05);
            }
            if (l.isKeyDown(KeyEvent.VK_U)) {
                rotation.setX(rotation.getX() - .05);
            }
            if (l.isKeyDown(KeyEvent.VK_O)) {
                rotation.setX(rotation.getX() + .05);
            }
            tracer.getCamera().setRotation(rotation);


            if(this.rot != null) {
                if (l.isKeyDown(KeyEvent.VK_W)) {
                    rot.addY(-.05);
                }
                if (l.isKeyDown(KeyEvent.VK_S)) {
                    rot.addY(.05);
                }
                if (l.isKeyDown(KeyEvent.VK_D)) {
                    rot.addZ(.05);
                }
                if (l.isKeyDown(KeyEvent.VK_A)) {
                    rot.addZ(-.05);
                }
                if (l.isKeyDown(KeyEvent.VK_UP)) {
                    rot.factorDistance(1/1.1);
                }
                if (l.isKeyDown(KeyEvent.VK_DOWN)) {
                    rot.factorDistance(1.1);
                }

                rot.apply();
            }
            tracer.getScene().tick();

        }

        actor.tick();
    }

    public GameListener getListener() {
        return parent.getListener();
    }

    public void executeCommand(String command) {
        String[] split = command.split(" ");
        Camera camera = this.tracer.getCamera();
        Vector3d rotation = this.tracer.getCamera().getRotation();
        Vector3d position = this.tracer.getCamera().getPosition();
        switch (split[0]) {
            case "move":
                double d = .1;
                if(split.length == 3) {
                    d = Double.parseDouble(split[2]);
                }
                switch (split[1]) {
                    case "+f": camera.moveForward(d); break;
                    case "-f": camera.moveForward(-d); break;
                    case "+s": camera.moveSideward(d); break;
                    case "-s": camera.moveSideward(-d); break;
                    case "+u": camera.moveUpwards(d); break;
                    case "-u": camera.moveUpwards(-d); break;
                    default: throw new RuntimeException("Invalid parameter: " + split[1]);
                }
                break;

            case "rotate":
            case "mode":
            case "exit":
            case "save": break;
            default:
                throw new RuntimeException("Unknown command:" + split[0]);
        }
        //assert false: command;
    }

    public void setMode(Actor.Mode mode) {
        this.mode = mode;
    }

    @Override
    public void keyPressed(int code) {
        if(code == KeyEvent.VK_R) {
            if(this.rot == null) {
                rot = new FixRotator();
            } else {
                this.rot = null;
            }
        }
    }

    @Override
    public void keyReleased(int code) {
    }

    @Override
    public void mouseClicked(int x, int y, int mouseButton) {
        if (mouseButton == MouseEvent.BUTTON1) {
            Ray ray = tracer.getCamera().getRay((x - tracer.getRenderWidth() / 2.0) / tracer.getRenderWidth(),
                    (y - tracer.getRenderHeight() / 2.0) / tracer.getRenderHeight());
            Intersection intersection = tracer.getIntersection(ray);
            if (intersection != null) {
                this.selectedShape = intersection.getShape();
            } else {
                this.selectedShape = null;
            }
        }
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public Settings getSettings() {
        return settings;
    }
}
