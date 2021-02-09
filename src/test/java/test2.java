import org.imei.ImeiDataAdmin;

public class test2 {
    public static void main(String[] args) {
        ImeiDataAdmin admin = new ImeiDataAdmin();
        System.out.println("insert:" + admin.setImei("label1", "861234567890123"));
        System.out.println("contain:" + admin.getIfExists("label1", "861234567890123"));
        System.out.println("not contain:" + admin.getIfExists("label1", "861234567890222"));
    }
}
