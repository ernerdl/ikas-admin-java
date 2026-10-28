package tr.com.sevinclidoga.ikas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bağımlılıksız, küçük JSON çözücü/yazıcı (RFC 8259). Nesne → {@code LinkedHashMap}, dizi → {@code ArrayList},
 * sayı → {@code BigDecimal}, metin → {@code String}, true/false → {@code Boolean}, null → {@code null}.
 */
public final class Json {

    private Json() {}

    /** JSON metnini çözer. Geçersiz metinde {@link IllegalArgumentException}. */
    public static Object coz(String metin) {
        Cozucu c = new Cozucu(metin);
        c.bosluk();
        Object v = c.deger(0);
        c.bosluk();
        if (c.i != metin.length()) throw c.hata("fazla karakter");
        return v;
    }

    /** Değeri JSON metnine yazar (Map, Collection, dizi değil; String, Number, Boolean, null, {@link IkasVeri}). */
    public static String yaz(Object v) {
        StringBuilder sb = new StringBuilder();
        yaz(sb, v);
        return sb.toString();
    }

    private static void yaz(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof IkasVeri d) {
            yaz(sb, d.ham());
        } else if (v instanceof String s) {
            metin(sb, s);
        } else if (v instanceof Enum<?> e) {
            metin(sb, e.name());
        } else if (v instanceof Boolean || v instanceof Integer || v instanceof Long || v instanceof Short) {
            sb.append(v);
        } else if (v instanceof BigDecimal b) {
            sb.append(b.toPlainString());
        } else if (v instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) throw new IllegalArgumentException("JSON'da NaN/sonsuz olmaz");
            sb.append(BigDecimal.valueOf(d).stripTrailingZeros().toPlainString());
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean ilk = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!ilk) sb.append(',');
                ilk = false;
                metin(sb, String.valueOf(e.getKey()));
                sb.append(':');
                yaz(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Collection<?> c) {
            sb.append('[');
            boolean ilk = true;
            for (Object o : c) {
                if (!ilk) sb.append(',');
                ilk = false;
                yaz(sb, o);
            }
            sb.append(']');
        } else {
            metin(sb, v.toString());
        }
    }

    private static void metin(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    private static final class Cozucu {
        private static final int EN_DERIN = 512;
        final String s;
        int i;

        Cozucu(String s) {
            this.s = s;
        }

        IllegalArgumentException hata(String m) {
            return new IllegalArgumentException("JSON çözülemedi (" + m + ", konum " + i + ")");
        }

        void bosluk() {
            while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\n' || s.charAt(i) == '\r' || s.charAt(i) == '\t')) i++;
        }

        Object deger(int derinlik) {
            if (derinlik > EN_DERIN) throw hata("çok derin");
            if (i >= s.length()) throw hata("beklenmeyen son");
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> nesne(derinlik);
                case '[' -> dizi(derinlik);
                case '"' -> metin();
                case 't' -> sabit("true", Boolean.TRUE);
                case 'f' -> sabit("false", Boolean.FALSE);
                case 'n' -> sabit("null", null);
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) yield sayi();
                    throw hata("beklenmeyen '" + c + "'");
                }
            };
        }

        Object sabit(String k, Object v) {
            if (!s.startsWith(k, i)) throw hata("beklenen " + k);
            i += k.length();
            return v;
        }

        Map<String, Object> nesne(int d) {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            bosluk();
            if (i < s.length() && s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                bosluk();
                if (i >= s.length() || s.charAt(i) != '"') throw hata("anahtar bekleniyor");
                String k = metin();
                bosluk();
                if (i >= s.length() || s.charAt(i) != ':') throw hata("':' bekleniyor");
                i++;
                bosluk();
                m.put(k, deger(d + 1));
                bosluk();
                if (i >= s.length()) throw hata("'}' bekleniyor");
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw hata("',' ya da '}' bekleniyor");
            }
        }

        List<Object> dizi(int d) {
            List<Object> l = new ArrayList<>();
            i++;
            bosluk();
            if (i < s.length() && s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                bosluk();
                l.add(deger(d + 1));
                bosluk();
                if (i >= s.length()) throw hata("']' bekleniyor");
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw hata("',' ya da ']' bekleniyor");
            }
        }

        String metin() {
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (i >= s.length()) break;
                char e = s.charAt(i++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw hata("eksik \\u");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw hata("geçersiz kaçış \\" + e);
                }
            }
            throw hata("kapanmayan metin");
        }

        BigDecimal sayi() {
            int bas = i;
            if (s.charAt(i) == '-') i++;
            while (i < s.length() && "0123456789.eE+-".indexOf(s.charAt(i)) >= 0) i++;
            try {
                return new BigDecimal(s.substring(bas, i));
            } catch (NumberFormatException e) {
                throw hata("geçersiz sayı");
            }
        }
    }
}
