package kairos.leeway.app.mapper;

import org.jnativehook.keyboard.NativeKeyEvent;

import java.lang.reflect.Field;

public class KeyCodeMapper {

    public static int getKeyCode(String keyText) {
        try {
            Field field = NativeKeyEvent.class.getField("VC_" + keyText.toUpperCase());
            return field.getInt(null);
        } catch (Exception e) {
            System.out.println("Invalid key: " + keyText);
            return NativeKeyEvent.VC_UNDEFINED;  // 未定义的键码
        }
    }
}
