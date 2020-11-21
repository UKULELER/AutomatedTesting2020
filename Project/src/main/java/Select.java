import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.*;
import java.util.*;

public class Select {

    private static Set<String> testMethods;//测试方法签名集合(InnerClassName+" "+signature)

    public static void main(String[] args) {
        try {
            // 参数组成： 命令 + <project_target> + <change_info>
            String cmd = args[0];
            String targetPath = args[1];
            String changeInfoPath = args[2];

            if (!cmd.equals("-c") && !cmd.equals("-m")){
                System.out.println("Useless command.");
            }
            else{
                if (targetPath.charAt(targetPath.length()-1)=='\\'){
                    targetPath = targetPath.substring(0,targetPath.length()-1);
                }
                // 创建method.dot文件
                makeMethodDot(targetPath);
                // 在method.dot文件的基础上创建class.dot文件
                makeClassDot();
                // -c执行类级别测试选择,-m执行方法级别测试选择
                if (cmd.equals("-c")){
                    makeClassSelection(changeInfoPath);
                }
                else{
                    makeMethodSelection(changeInfoPath);
                }
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    /**
     * 创建 method-cfa.dot
     * @param target
     * */
    public static void makeMethodDot(String target) throws Exception{

        // 构建分析域对象scope
        AnalysisScope scope = AnalysisScopeReader.readJavaScope
                ("scope.txt", new File("exclusion.txt"), Select.class.getClassLoader());

        File[] classes = new File(target+ "\\classes\\net\\mooctest").listFiles();
        File[] testClasses = new File(target+"\\test-classes\\net\\mooctest").listFiles();

        if(classes!=null) {
            for (File clazz : classes) {
                System.out.println(clazz.toString());
                scope.addClassFileToScope(ClassLoaderReference.Application, clazz);
            }
        }

        if(testClasses!=null) {
            for (File testClass : testClasses) {
                System.out.println(testClass.toString());
                scope.addClassFileToScope(ClassLoaderReference.Application, testClass);
            }
        }

        // 生成类层次关系对象
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        // 生成进入点
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
        // 利用CHA算法构建调用图
        CHACallGraph cg = new CHACallGraph(cha);
        cg.init(eps);

        // 记录下所有测试方法的签名集合
        testMethods = getSignatureOfTestMethods(cg);

        // 创建文件method-cfa.dot
        File file = new File("method-cfa.dot");
        file.createNewFile();

        // 写文件method-cfa.dot
        FileWriter writer = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        bufferedWriter.write("digraph method {\r\n");

        // 遍历cg中所有的节点
        // node中包含很多信息，包括类加载器、方法信息等，这里只筛选出需要的信息
        for (CGNode node : cg) {
            // 一般地，本项目中所有和业务逻辑相关的方法都是ShrikeBTMethod对象
            // 对caller（调用者）和callee（被调用者）皆需判断是否为ShrikeBTMethod和"Application"
            if (node.getMethod() instanceof ShrikeBTMethod) {
                // node.getMethod()返回一个比较泛化的IMethod实例，不能获取到我们想要的信息
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                // 使用Primordial类加载器加载的类都属于Java原生类，我们一般不关心。
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())){
                    Iterator<CGNode> iterator = cg.getPredNodes(node);
                    while(iterator.hasNext()){
                        CGNode callee = iterator.next();
                        if (callee.getMethod() instanceof ShrikeBTMethod){
                            if ("Application".equals(callee.getMethod().getDeclaringClass().getClassLoader().toString())){
                                // 判断是否是net.mooctest包下的方法，若不是则剔除
                                if (isInNetMooctest(callee.getMethod().getSignature()) && isInNetMooctest(node.getMethod().getSignature())){
                                    bufferedWriter.write(String.format("\t\"%s\" -> \"%s\";\r\n",node.getMethod().getSignature(),callee.getMethod().getSignature()));
                                }
                            }
                        }
                        // 后继
//                    Iterator<CGNode> iterator = cg.getSuccNodes(node);
//                    while(iterator.hasNext()){
//                        CGNode caller = iterator.next();
//                        if (caller.getMethod() instanceof ShrikeBTMethod){
//                            if ("Application".equals(caller.getMethod().getDeclaringClass().getClassLoader().toString())){
//                                if (isNetMooctest(caller.getMethod().getSignature()) && isNetMooctest(node.getMethod().getSignature())){
//                                    bufferedWriter.write(String.format("\t\"%s\" -> \"%s\";\r\n",caller.getMethod().getSignature(),node.getMethod().getSignature()));
//                                }
//                            }
//                        }
                    }
                    bufferedWriter.flush();
                }
            }
        }
        bufferedWriter.write("}\r\n");
        bufferedWriter.flush();
        bufferedWriter.close();

        System.out.println("method-cfa.dot is ready.");
        return;
    }

    /**
     * 得到所有的测试方法
     */
    public static Set<String> getSignatureOfTestMethods(CHACallGraph cg) {
        Set<String> testMethods = new HashSet<String>();
        for (CGNode node : cg) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                // node.getMethod()返回一个比较泛化的IMethod实例，不能获取到我们想要的信息
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                // 使用Primordial类加载器加载的类都属于Java原生类，我们一般不关心。
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String signature = method.getSignature();
                    //用反射机制拿到@Test注解来判断是不是测试方法
                    if (isTest(method)) {
                        testMethods.add(classInnerName + " " + signature);
                    }
                }
            } else {
                System.out.println(String.format("'%s'不是一个ShrikeBTMethod：%s", node.getMethod(), node.getMethod().getClass()));
            }
        }
        return testMethods;
    }


    /**
     * 创建class-cfa.dot，通过读取method-cfa.dot文件来生成class-cfa.dot
     * */
    public static void makeClassDot() throws Exception{
        Map<String,HashSet<String>> hashMap = new HashMap<>();
        HashSet<String> set = new HashSet<>();
        String line, caller_class, callee_class;
        String[] temp;

        BufferedReader reader = new BufferedReader(new FileReader("method-cfa.dot"));
        while ((line = reader.readLine()) != null) {
            if (line.equals("digraph method {") || line.equals("}")){
                continue;
            }
            line = line.substring(1,line.length()-1);  //去除首部\t,尾部;
            temp = line.split(" -> ");
            if (temp.length != 2){
                System.out.println("method.dot is wrong.");
                break;
            }
            //去除首尾""
            caller_class = temp[0].substring(1,temp[0].length()-1);
            callee_class = temp[1].substring(1,temp[1].length()-1);
            temp = caller_class.split("\\.");
            // 其中索引0：net 索引1：mooctest 索引2：类名
            caller_class = "L" + temp[0] + "/" + temp[1] + "/" + temp[2];
            temp = callee_class.split("\\.");
            callee_class = "L" + temp[0] + "/" + temp[1] + "/" + temp[2];

            if (hashMap.get(caller_class) != null){
                // 若已经在hashMap中，则拿出value代表的Set,加在尾部再存放
                set = hashMap.get(caller_class);
                set.add(callee_class);
                hashMap.put(caller_class,set);
                set = new HashSet<>();
            }
            else{
                set.add(callee_class);
                hashMap.put(caller_class,set);
                set = new HashSet<>();
            }
        }


        // 写入文件class-cfa.dot
        FileWriter writer = new FileWriter(new File("class-cfa.dot"));
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        bufferedWriter.write("digraph class {\r\n");
        Iterator iterator;
        String valuetemp, keytemp;
        //由hashMap来生成class-cfa.dot
        for (Map.Entry<String, HashSet<String>> entry : hashMap.entrySet()) {
            keytemp = entry.getKey();
            iterator = entry.getValue().iterator();
            while (iterator.hasNext()){
                valuetemp = (String) iterator.next();
                bufferedWriter.write(String.format("\t\"%s\" -> \"%s\";\r\n",keytemp, valuetemp));
                bufferedWriter.flush();
            }
        }
        bufferedWriter.write("}\r\n");
        bufferedWriter.flush();
        bufferedWriter.close();

        System.out.println("class-cfa.dot is ready.");
    }


    /**
     * 创建selection-class.txt
     * @param path change_info.txt的路径
     * */
    public static void makeClassSelection(String path) throws Exception{

        HashSet<String> classChangeSet = new HashSet<>();
        String line, caller_origin, caller_class, callee;
        String[] temp;

        Map<String, HashSet<String>> changeInfo = readChangeInfo(path);
        // 将change_info.txt中改变的类加入classChangeSet中
        for (Map.Entry<String, HashSet<String>> entry : changeInfo.entrySet()) {
            classChangeSet.add(entry.getKey());
        }

        // 读取class-cfa.dot
        BufferedReader classReader = new BufferedReader(new FileReader("class-cfa.dot"));

        int count = -1; // 用于计算本次循环是否还有关联其他方法，若无则在下次循环中跳出
        // 将全部class-cfa.dot中涉及到的类（与改变类相关的）加入到classChangeSet中
        while (count != 0) {
            count = 0;
            while ((line = classReader.readLine()) != null){
                if (line.equals("digraph class {") || line.equals("}")){
                    continue;
                }
                line = line.substring(1,line.length()-1);
                temp = line.split(" -> ");

                assert temp.length == 2;

                caller_origin = temp[0].substring(1,temp[0].length()-1);
                callee = temp[1].substring(1,temp[1].length()-1);

                if (classChangeSet.contains(caller_origin)){
                    // 如果被调用类不存在于classChangeSet中才持续循环（count != 0）
                    if (!classChangeSet.contains(callee)){
                        classChangeSet.add(callee);
                        count++;
                    }
                }
            }
        }

        // 打开文件selection-class.txt
        FileWriter writer = new FileWriter(new File("selection-class.txt"));
        BufferedWriter bufferedWriter = new BufferedWriter(writer);

        HashSet<String> res = new HashSet<>();
        Hashtable<String, Set<String>> methodsUnderTestClass = recordMethodsUnderTestClass(testMethods);

        // 遍历所有的改变影响到的class
        for (String classChanged : classChangeSet) {
            //该测试类下的所有测试方法都要选中
            if(methodsUnderTestClass.get(classChanged)!=null) {
                for (String testMethod : methodsUnderTestClass.get(classChanged)) {
                    res.add(classChanged + " " + testMethod);
                }
            }
        }

        // 写入文件selection-class.txt
        Iterator iterator = res.iterator();
        while (iterator.hasNext()){
            bufferedWriter.write(String.format("%s\r\n",iterator.next()));
            bufferedWriter.flush();
        }
        bufferedWriter.close();

        System.out.println("selection-class.txt is ready.");
    }

    /**
     * 记录所有的类和旗下的测试方法
     * @param testMethods 测试方法签名集合(InnerClassName+" "+signature)
     */
    public static Hashtable<String, Set<String>> recordMethodsUnderTestClass(Set<String> testMethods) {
        Hashtable<String, Set<String>> hashtable = new Hashtable<>();
        for (String testMethod : testMethods) {
            String testClass = testMethod.split(" ")[0];//测试方法所属的测试类
            if (!hashtable.containsKey(testClass)) {
                hashtable.put(testClass, new HashSet<String>());
            }
            Set<String> methodsUnderTestClass = hashtable.get(testClass);
            methodsUnderTestClass.add(testMethod.split(" ")[1]);
        }
        return hashtable;
    }

    /**
     * 创建selection-method.txt
     * @param path change_info.txt的路径
     * */
    public static void makeMethodSelection(String path) throws Exception{
        Map<String, HashSet<String>> changeInfo = readChangeInfo(path);
        List<String> removeList = new ArrayList<>();   //用于最后排除掉初始的changeInfo
        List<String> classList = new ArrayList<>();
        HashSet<String> methodSet = new HashSet<>();

        String line, str, caller_origin, callee_origin;
        String[] temp;

        for (Map.Entry<String, HashSet<String>> entry : changeInfo.entrySet()) {
            Iterator iterator = entry.getValue().iterator();
            while (iterator.hasNext()){
                str = (String) iterator.next();
                removeList.add(str);
                methodSet.add(str);
            }
        }

        // 读取文件method-cfa.dot
        BufferedReader reader;

        // 写入文件select-class.txt
        FileWriter writer = new FileWriter(new File("selection-method.txt"));
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        HashSet<String> tempSet;
        String next;

        int count = -1;  // 用于计算本次循环是否还有关联其他方法，若无则在下次循环中跳出
        // 循环读取method-cfa.dot,用于找出所有与change_info.txt相关联的方法
        while(count != 0) {
            count = 0;
            tempSet = new HashSet<>();
            reader = new BufferedReader(new FileReader("method-cfa.dot"));
            // 完整读完一次method-cfa.dot
            while ((line = reader.readLine()) != null) {
                if (line.equals("digraph method {") || line.equals("}")){
                    continue;
                }
                // 去除首尾 \t ;
                line = line.substring(1,line.length()-1);
                temp = line.split(" -> ");

                assert temp.length == 2;

                // 去除首尾""
                caller_origin = temp[0].substring(1,temp[0].length()-1);
                callee_origin = temp[1].substring(1,temp[1].length()-1);

                if (methodSet.contains(caller_origin)){
                    tempSet.add(callee_origin);
                }
            }

            Iterator iterator = tempSet.iterator();
            while (iterator.hasNext()){
                next = (String) iterator.next();
                if (methodSet.contains(next)){
                    continue;
                }
                methodSet.add(next);
                count++;
            }
        }

        // 最终输出，但要排除初始changeInfo
        for (int i=0; i<removeList.size(); i++){
            assert methodSet.remove(removeList.get(i));
        }

        // 最终输出，与change_info.txt相关联的类也不必输出
        for (int i=0; i<removeList.size(); i++){
            str = removeList.get(i);
            temp = str.split("\\.");
            classList.add("L"+temp[0]+"/"+temp[1]+"/"+temp[2]);
        }

        Iterator iterator = methodSet.iterator();
        while (iterator.hasNext()){
            next = (String) iterator.next();
            temp = next.split("\\.");
            // 不是测试类，或者方法为<init>()剔除
            if (temp[2].contains("Test") && !next.contains("<init>()")){
                str = "L" + temp[0] + "/" + temp[1] + "/" + temp[2];
                if (!classList.contains(str)){
                    bufferedWriter.write(String.format("%s %s\r\n",str, next));
                    bufferedWriter.flush();
                }
            }
        }
        bufferedWriter.close();

        System.out.println("selection-method.txt is ready.");
    }


    /**
     * 读取change_info.txt内容
     * */
    public static Map<String, HashSet<String>> readChangeInfo(String changeInfo) throws Exception{
        Map<String, HashSet<String>> source = new HashMap<>();
        HashSet<String> set = new HashSet<>();
        String line;
        String[] info;

        BufferedReader reader = new BufferedReader(new FileReader(changeInfo));
        while ((line = reader.readLine()) != null) {
            info = line.split(" ");
            // 检验文件格式
            if (info.length != 2) {
                System.out.println("change_info.txt is wrong.");
                break;
            }
            //索引0存放类，索引1存放方法
            if (source.get(info[0]) != null) {
                // 此时key存在
                set = source.get(info[0]);
                set.add(info[1]);
                source.put(info[0], set);
                set = new HashSet<>();
            }
            else {
                set.add(info[1]);
                source.put(info[0], set);
                set = new HashSet<>();
            }
        }

        // 返回类型Map,其中key代表caller, value代表a set of callee
        return source;
    }


    /**
     * 判断该类是否为net.mooctest包下的类
     * */
    public static boolean isInNetMooctest(String string){
        try{
            if (string.substring(0,12).equals("net.mooctest")){
                return true;
            }
        }
        catch(Exception e){
            System.out.println("length is less than 11");
            return false;
        }
        return false;
    }

    /**
     * 通过反射机制拿到注解判断是否是测试方法
     */
    public static boolean isTest(ShrikeBTMethod method) {
        Collection<Annotation> annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            if (new String(annotation.getType().getName().getClassName().getValArray()).equals("Test")) {
                return true;
            }
        }
        return false;
    }

}
