import org.junit.Test;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class PersonalTest {

    @Test
    public void ALUTest() throws IOException {
        Set<String> classLevelRes = loadFile(".\\Report\\1-ALU\\selection-class.txt");
        Set<String> methodLevelRes = loadFile(".\\Report\\1-ALU\\selection-method.txt");
        Set<String> classLevelData = loadFile( ".\\Data\\1-ALU\\data\\selection-class.txt");
        Set<String> methodLevelData = loadFile(".\\Data\\1-ALU\\data\\selection-method.txt");
        assertTrue(cmp(classLevelRes, classLevelData));
        assertTrue(cmp(methodLevelRes, methodLevelData));
    }

    @Test
    public void DataLogTest() throws IOException {
        Set<String> classLevelRes = loadFile("Report\\2-DataLog\\selection-class.txt");
        Set<String> methodLevelRes = loadFile("Report\\2-DataLog\\selection-method.txt");
        Set<String> classLevelData = loadFile( "Data\\2-DataLog\\data\\selection-class.txt");
        Set<String> methodLevelData = loadFile("Data\\2-DataLog\\data\\selection-method.txt");
        assertTrue(cmp(classLevelRes, classLevelData));
        assertTrue(cmp(methodLevelRes, methodLevelData));
    }

    @Test
    public void BinaryHeapTest() throws IOException {
        Set<String> classLevelRes = loadFile("Report\\3-BinaryHeap\\selection-class.txt");
        Set<String> methodLevelRes = loadFile("Report\\3-BinaryHeap\\selection-method.txt");
        Set<String> classLevelData = loadFile( "Data\\3-BinaryHeap\\data\\selection-class.txt");
        Set<String> methodLevelData = loadFile("Data\\3-BinaryHeap\\data\\selection-method.txt");
        assertTrue(cmp(classLevelRes, classLevelData));
        assertTrue(cmp(methodLevelRes, methodLevelData));
    }

    @Test
    public void NextDayTest() throws IOException {
        Set<String> classLevelRes = loadFile("Report\\4-NextDay\\selection-class.txt");
        Set<String> methodLevelRes = loadFile("Report\\4-NextDay\\selection-method.txt");
        Set<String> classLevelData = loadFile( "Data\\4-NextDay\\data\\selection-class.txt");
        Set<String> methodLevelData = loadFile("Data\\4-NextDay\\data\\selection-method.txt");
        assertTrue(cmp(classLevelRes, classLevelData));
        assertTrue(cmp(methodLevelRes, methodLevelData));
    }

    @Test
    public void MoreTriangleTest() throws IOException {
        Set<String> classLevelRes = loadFile("Report\\5-MoreTriangle\\selection-class.txt");
        Set<String> methodLevelRes = loadFile("Report\\5-MoreTriangle\\selection-method.txt");
        Set<String> classLevelData = loadFile( "Data\\5-MoreTriangle\\data\\selection-class.txt");
        Set<String> methodLevelData = loadFile("Data\\5-MoreTriangle\\data\\selection-method.txt");
        assertTrue(cmp(classLevelRes, classLevelData));
        assertTrue(cmp(methodLevelRes, methodLevelData));
    }

    private boolean cmp(Set<String> res, Set<String> data) {
        System.out.println("res.size is "+res.size());
        System.out.println("data.size is "+(data.size()-1));
        if (res.size() != data.size()-1) {
            return false;
        }
        for (String s : res) {
            if (!data.contains(s)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> loadFile(String path) throws IOException {
        File file = new File(path);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        Set<String> changedMethods = new HashSet<String>();
        String s;
        while ((s = bufferedReader.readLine()) != null) {
            changedMethods.add(s.trim());
        }
        return changedMethods;
    }

}