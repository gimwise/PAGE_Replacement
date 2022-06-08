import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class UI extends JFrame {

    public static int HIT = 1;
    public static int PAGE_FAULT = 2;
    public static int KICK = 3;

    private JFrame frame;
    private JPanel excelPanel;
    private JTextField patternField;
    private JTextField frameField;
    private JTextArea chartField;
    private JTextArea logField;
    private JComboBox comboBox;
    private JButton ranButton;
    private JButton runButton;

    private JLabel frameLabel;
    private JLabel patternLabel;
    private JLabel pageFaultLabel;


    private JScrollPane chartScrollPane;
    private JScrollPane logScrollPane;

    public int frameSize = 0; // 메모리 크기
    public int hit = 0; // hit 횟수
    public int kick = 0; // kick 횟수
    public int fault = 0; // fault 횟수
    public int onMem = 0; // 메모리에 올라와있는 페이지 개수
    int index = -1;

    ArrayList<Page> list = new ArrayList<Page>(); // FIFO, OPTIMAL
    HashMap<Character, Integer> map = new HashMap<Character, Integer>(); // LFU, MFU 구현시 참조 횟수를 count하는 hashmap


    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UI window = new UI();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public UI() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 900, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);

        comboBox = new JComboBox();
        comboBox.setBounds(15, 30, 90, 30);
        frame.getContentPane().add(comboBox);

        comboBox.addItem("FIFO");
        comboBox.addItem("OPTIMAL");
        comboBox.addItem("LRU");
        comboBox.addItem("LFU");
        comboBox.addItem("MFU");

        frameLabel = new JLabel("프레임 개수");
        frameLabel.setBounds(120, 30, 100, 30);
        frame.getContentPane().add(frameLabel);

        frameField = new JTextField();
        frameField.setColumns(10);
        frameField.setBounds(180, 30, 50, 30);
        frame.getContentPane().add(frameField);
        frameField.setText("4");

        patternLabel = new JLabel("패턴");
        patternLabel.setBounds(250, 30, 100, 30);
        frame.getContentPane().add(patternLabel);

        patternField = new JTextField();
        patternField.setBounds(275, 30, 300, 30);
        patternField.setColumns(5);
        frame.getContentPane().add(patternField);

        ranButton = new JButton("랜덤 패턴");
        ranButton.setBounds(590, 30, 100, 30);
        frame.getContentPane().add(ranButton);
        ranButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Random rand = new Random(); // 자릿 수

                int digit = rand.nextInt(25) + 8;
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < digit; i++) {
                    char ch = (char) ((Math.random() * 26) + 65);
                    sb.append(ch);
                }

                patternField.setText(sb.toString());
            }
        });

        runButton = new JButton("실행");
        runButton.setBounds(700, 30, 100, 30);
        frame.getContentPane().add(runButton);
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                init();
                StringBuilder sb = new StringBuilder();
                int state = 0; // hit 1, page fault 2, kick 3
                String patterns = patternField.getText();
                if (frameField.getText() == null) {
                    return;
                }
                frameSize = Integer.parseInt(frameField.getText()); // 프레임 크기

                for (int i = 0; i < patterns.length(); i++) {
                    int j = 0;
                    char page = patterns.charAt(i);

                    if (comboBox.getSelectedItem() == "FIFO") {
                        state = FIFO(page);

                    } else if (comboBox.getSelectedItem() == "OPTIMAL") {
                        state = OPTIMAL(page, i);

                    } else if (comboBox.getSelectedItem() == "LRU") {
                        state = LRU(page, i);

                    } else if (comboBox.getSelectedItem() == "LFU"){
                        state = LFU(page, i);

                    }
                    else {
                        state = MFU(page,i);
                    }

                    for (Page p : list) {
                        char c = p.data;
                        j++;
                        sb.append(c + " ");
                    }
                    for (int k = j; k < frameSize; k++) {
                        sb.append("* ");
                    }
                    sb.append("\n");
                    chartField.setText(sb.toString());

                    switch (state){
                        case 1 :
                            logField.append(page + " is HIT\n");
                            break;
                        case 2:
                            logField.append(page + " is FAULT\n");
                            break;
                        case 3 :
                            logField.append(page + " is KICK another page\n");
                            break;
                    }
                    pageFaultLabel.setText("Fault Rate : " + (fault * 100 / (hit + fault)) +"%");
                }
                makeChart();

            }
        });

        chartField = new JTextArea();
        chartScrollPane = new JScrollPane(chartField);
        chartScrollPane.setBounds(15, 80, 250, 500);
        frame.getContentPane().add(chartScrollPane);

        // 페이지 상태
        logField = new JTextArea();
        logScrollPane = new JScrollPane(logField);
        logScrollPane.setBounds(330, 80, 250, 500);
        frame.getContentPane().add(logScrollPane);

        excelPanel = new JPanel();
        excelPanel.setBounds(610, 200, 250, 200);
        frame.getContentPane().add(excelPanel);

        pageFaultLabel = new JLabel("");
        pageFaultLabel.setBounds(610, 80, 200, 30);
        frame.getContentPane().add(pageFaultLabel);
    }

    public void init() {
        chartField.setText("");
        logField.setText("");
        excelPanel.removeAll();
        list.clear();
        map.clear();
        hit = 0;
        fault = 0;
        kick = 0;
        onMem = 0;

    }


//    70120304230321201701
    public int FIFO(char page) {
        Page newPage = new Page();

        // HIT
        for (Page p : list) {
            char c = p.data;
            if (c == page) {
                hit++;
                return HIT;
            }
        }
        // 페이지 정보 저정
        newPage.pid = Page.pageCnt++;
        newPage.data = page;

        // 메모리에 올라와있는 페이지가 없으면 가장 최근에 들어온 페이지가 가장 오래된 페이지가 됨
        if (onMem == 0) {
            index = 0;
        }
        // FAULT -> 메모리가 남아있다면 메모리에 페이지를 올림
        if (onMem < frameSize) {
            onMem++;
            list.add(newPage);
            fault++;
            return PAGE_FAULT;
        }
        // KICK -> 메모리가 꽉 찼다면 하나는 쫓겨나야함
        else {
            list.set(index, newPage); // 제일 먼저 들어온 페이지를 지우고
            // 가장 오래된 페이지 수정
            if (index == frameSize - 1) {
                index = 0;
            } else {
                index++;
            }
            kick++;
            fault++;
            return KICK;
        }
    }

    public int OPTIMAL(char page, int curIndex) {
        int maxIndex = 0;
        int[] arr = new int[frameSize];
        Arrays.fill(arr, -999);
        String patterns = patternField.getText();
        Page newPage = new Page();

        for (Page temp : list) {
            char c = temp.data;
            // HIT
            if (c == page) {
                hit++;
                return HIT;
            }
        }
        newPage.pid = Page.pageCnt++;
        newPage.data = page;
        // page fault
        if (onMem < frameSize) {
            onMem++;
            list.add(newPage);
            fault++;
            return PAGE_FAULT;
        }
        // kick
        else {
            for(int i=0 ; i<frameSize; i++){
                for(int j = curIndex+1 ; j<patterns.length() ; j++){
                    if(arr[i] != -999){
                        break;
                    }
                    if(list.get(i).data == patterns.charAt(j)){
                        arr[i] = j - curIndex;
                    }
                }
            }

            int flagNum = 0;
            for(int j=0 ; j<frameSize ; j++){
                if(arr[j] == -999){
                    flagNum++;
                }
            }

            if(flagNum == 1){
                for(int i=0 ; i<frameSize ; i++){
                    if(arr[i] == -999){
                        arr[i] = 999;
                    }
                }
            }
            else if(flagNum > 1){
                for(int i = 0 ; i<frameSize ; i++){
                    for(int j = 0; j<patterns.length() ; j++){
                        if(arr[i] != -999){
                            break;
                        }
                        if(list.get(i).data == patterns.charAt(j)){
                            arr[i] = j - curIndex;
                        }
                    }
                }
            }

            int max = arr[0];
            for(int k=0 ; k<arr.length; k++){
                if(Math.abs(arr[k]) > max){
                    max = Math.abs(arr[k]);
                    maxIndex = k;
                }
            }

            list.set(maxIndex, newPage);
            kick++;
            fault++;

            return KICK;
        }
    }

    public int LRU(char page, int curIndex){
        int maxIndex = 0;
        int[] arr = new int[frameSize];
        Arrays.fill(arr, -999);
        String patterns = patternField.getText();
        Page newPage = new Page();

        for (Page temp : list) {
            char c = temp.data;
            // HIT
            if (c == page) {
                hit++;
                return HIT;
            }
        }

        newPage.pid = Page.pageCnt++;
        newPage.data = page;

        // page fault
        if (onMem < frameSize) {
            onMem++;
            list.add(newPage);
            fault++;
            return PAGE_FAULT;
        }
        // kick
        else {
            for (int i = 0; i < frameSize; i++) {
                for (int j = curIndex-1; j > -1 ; j--) {
                    if (arr[i] != -999) {
                        break;
                    }
                    if (list.get(i).data == patterns.charAt(j)) {
                        arr[i] = j - curIndex;
                    }
                }
            }

            int max = arr[0];
            for (int k = 0; k < arr.length; k++) {
                if (Math.abs(arr[k]) > max) {
                    max = Math.abs(arr[k]);
                    maxIndex = k;
                }
            }

            list.set(maxIndex, newPage);
            kick++;
            fault++;

            return KICK;
        }
    }

    public int LFU(char page, int curIndex){
        int maxIndex = 0;
        int[] arr = new int[frameSize];
        int[] last = new int[frameSize];
        Arrays.fill(arr, 0);
        String patterns = patternField.getText();
        Page newPage = new Page();

        for (Page temp : list) {
            char c = temp.data;
            // HIT
            if (c == page) {
                hit++;
                return HIT;
            }
        }

        newPage.pid = Page.pageCnt++;
        newPage.data = page;

        // page fault
        if (onMem < frameSize) {
            onMem++;
            list.add(newPage);
            fault++;
            return PAGE_FAULT;
        }
        // kick
        else {
            for (int i = 0; i < frameSize; i++) {
                for (int j = 0; j < curIndex ; j++) {
                    if (list.get(i).data == patterns.charAt(j)) {
                        arr[i]++;
                        last[i] = j - curIndex;
                    }
                }
            }

            int min = arr[0];
            int minLast = last[0];
            for (int k = 0; k < arr.length; k++) {
                if (Math.abs(arr[k]) < min || ((Math.abs(arr[k]) == min) && Math.abs(last[k]) > minLast)) {
                    min = Math.abs(arr[k]);
                    minLast = Math.abs(last[k]);
                    maxIndex = k;
                }
            }

            list.set(maxIndex, newPage);
            kick++;
            fault++;

            return KICK;
        }
    }

    public int MFU(char page, int curIndex){
        int maxIndex = 0;
        int[] arr = new int[frameSize];
        int[] last = new int[frameSize];
        Arrays.fill(arr, 0);
        String patterns = patternField.getText();
        Page newPage = new Page();

        for (Page temp : list) {
            char c = temp.data;
            // HIT
            if (c == page) {
                hit++;
                return HIT;
            }
        }

        newPage.pid = Page.pageCnt++;
        newPage.data = page;

        // page fault
        if (onMem < frameSize) {
            onMem++;
            list.add(newPage);
            fault++;
            return PAGE_FAULT;
        }
        // kick
        else {
            System.out.println("cur " + curIndex);
            for (int i = 0; i < frameSize; i++) {
                for (int j = 0; j < curIndex ; j++) {
                    if (list.get(i).data == patterns.charAt(j)) {
                        arr[i]++;
                        last[i] = j - curIndex;
                    }
                }
            }

            int max = arr[0];
            int maxLast = last[0];
            for (int k = 0; k < arr.length; k++) {
                if (Math.abs(arr[k]) > max || ((Math.abs(arr[k]) == max) && Math.abs(last[k]) > maxLast)) {
                    max = Math.abs(arr[k]);
                    maxLast = Math.abs(last[k]);
                    maxIndex = k;
                }
            }

            list.set(maxIndex, newPage);
            kick++;
            fault++;

            return KICK;
        }
    }

    public void makeChart() { // 차트 만들기
        PieDataset pieDataset = getDataset();

        JFreeChart jFreeChart = ChartFactory.createPieChart("", pieDataset, true, true, false); //차트 생성
        ChartPanel chartPanel = new ChartPanel(jFreeChart) {
            public Dimension getPreferredSize() {
                return new Dimension(220, 150);
            }
        };
        excelPanel.add(chartPanel);
        excelPanel.validate();
    }
    private PieDataset getDataset() {
        DefaultPieDataset defaultPieDataset = new DefaultPieDataset();
        defaultPieDataset.setValue("HIT", hit);
        defaultPieDataset.setValue("FAULT", fault - kick);
        defaultPieDataset.setValue("KICK", kick);
        return defaultPieDataset;
    }
}
