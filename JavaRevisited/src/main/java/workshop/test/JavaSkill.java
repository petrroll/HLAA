package workshop.test;

public class JavaSkill {
	
	public static void question1() {
		System.out.println("QUESTION 1");
		System.out.println("(2/4)*4 + 4%2 = " + ((2/4)*4 + 4%2));
	}
	
	public static void question2() {
		System.out.println("QUESTION 2");
		String s = "hello";
		System.out.print("System.out.print(s.charAt(0)) == ");
		System.out.println(s.charAt(0));
	}
	
	private static void question3() {
		System.out.println("QUESTION 3");
		String x = "xyz";
		x.toUpperCase(); 
		String y = x.replace('Y', 'y');
		y = y + "abc";
		System.out.println(y);
	}
	
	private static void question4(String[] args) {
		System.out.println("QUESTION 4");
		String s1 = args[1];
        String s2 = args[2];
        String s3 = args[3];
        String s4 = args[4];
        System.out.print(" args[2] = " + s2);
	}
	
	// QUESTION 5
	// Uncomment to see the compilation error
	
//	class Super
//	{ 
//	    public String text = "Hello 0"; 
//
//	    public Super(String text)
//	    {
//	        this.text = text +" "+ 1; 
//	    } 
//	} 
//
//	class Sub extends Super
//	{
//	    public Sub(String text)
//	    {
//	        this.text = text + " "+ 2; 
//	    } 
//
//	    public static void main(String args[])
//	    {
//	        Sub sub = new Sub("Hello"); 
//	        System.out.println(sub.text); 
//	    } 
//	}
	
	private static void question6() {
		System.out.println("QUESTION 6");
		int a = Math.abs(-5);
		System.out.println(a);
		// UNCOMMENT TO SEE COMPILATION ERRORS
//		int a = Math.abs(-5.0);
//		int a = Math.abs(-5.5F);
//		int a = Math.abs(-5L);
	}
	
	// QUESTION 7
	// UNCOMMENT TO SEE COMPILATION ERRORS
	
//	public class Question7{
//        private int i = j;
//        private int j = 10;
//        public static void myMain(String args[]){
//                System.out.println((new Question7()).i);
//        }
//	}
	
	// QUESTION 8
	public interface Question8 {
        public abstract void someMethod() throws Exception;
	}
	public class QuestionImpl implements Question8 {
		@Override
		public void someMethod() {
		}
	}
	
	// QUESTION 9
	public static class Question9 {
        public void method(Object o){
            System.out.println("Object Verion");
        }
		public void method(String s){
            System.out.println("String Version");
		}   
	}
	
	public static void question9() {
		System.out.println("QUESTION 9");
	    Question9 question = new Question9();
        question.method(null);
    }
	
	// QUESTION 10
	// UNCOMMENT TO SEE COMPILATION ERROR
	
//	public class Question10{
//        public void method(StringBuffer sb){
//                System.out.println("StringBuffer Verion");
//        }
//        public void method(String s){
//                System.out.println("String Version");
//        }
//	}
//	
//    public static void question10(String args[]){
//    	Question10 question = new Question10();
//        question.method(null);
//    }
	
	public static void question11() {
		System.out.println("QUESTION 11");
		try 
        { 
            return; 
        } 
        finally 
        {
            System.out.println( "Finally" ); 
        } 
	}
	
	public static void question12() {
		System.out.println("QUESTION 12");
		int i;
		for (i = 0; i < 4; i += 2) 
		{ 
		    System.out.print(i + " "); 
		} 
		System.out.println(i);
	}
	
	public static void main(String[] args) {
		question1();
		question2();
		question3();
		try {
			question4(new String[]{"1","2","3","4"});
		} catch (Exception e) {
			e.printStackTrace();
		}
		question6();
		question9();
		question11();
		question12();
	}

	
	

}
