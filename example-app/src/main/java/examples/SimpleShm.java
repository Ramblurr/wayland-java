package examples;

public class SimpleShm {

    public static void main(String args[]) {

        Native.loadLibrary("wayland-java-util");

        Display display = new Display();
        Window window = new Window(display,
                                   250,
                                   250);
        window.redraw(0);

        try {
            while (true) {
                display.getDisplayProxy()
                       .dispatch();
            }
        }
        catch (Exception e) {
            window.destroy();
            display.destroy();
            throw new RuntimeException(e);
        }
    }
}
