import org.roaringbitmap.RoaringBitmap;

public class test1 {
    public static void main(String[] args) {
        RoaringBitmap rbm1 = new RoaringBitmap();
        // 测试不同的container
        rbm1.add(1474976710656L);
        boolean b1 = rbm1.contains(1474976710656L);
        boolean b2 = rbm1.contains(1474976710657L);
        System.out.println("应该是true：" + b1);
        System.out.println("应该是false：" +b2);
        for (int i = 0; i < 100000; i++) {
            rbm1.add(1474976710656L + i);
        }
        boolean b3 = rbm1.contains(1474976710756L);
        boolean b4 = rbm1.contains(1574976710657L);
        System.out.println("应该是true：" + b3);
        System.out.println("应该是false：" +b4);
    }
}


