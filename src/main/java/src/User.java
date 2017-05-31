package src;

/**
 * Created by User on 029 29.05.17.
 */
public class User {
    private Long date;
    private String mes;

    public Long getKey() {
        return date;
    }

    public void setKey(Long key) {
        this.date = key;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public User(Long date, String mes) {
        this.date = date;
        this.mes = mes;
    }
}
