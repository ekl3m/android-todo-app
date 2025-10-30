package utilities;

import android.text.TextUtils;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Converters {
    @TypeConverter
    public static String fromList(List<String> list) {
        return TextUtils.join("|", list);
    }

    @TypeConverter
    public static List<String> toList(String data) {
        return data == null || data.isEmpty() ? new ArrayList<>() :
                new ArrayList<>(Arrays.asList(data.split("\\|")));
    }
}
