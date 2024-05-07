import java.io.*;
import java.nio.file.Path;
import java.util.*;

class PCB {
    int job_id;
    int TTL;
    int TLL;

    void initialise(String buffer) {
        this.job_id = Integer.parseInt(buffer.substring(4, 8));
        this.TTL = Integer.parseInt(buffer.substring(8, 12));
        this.TLL = Integer.parseInt(buffer.substring(12, 16));
    }

    void details() {
        System.out.println("Process created with Job Id " + this.job_id);
        System.out.println("Total Time Limit for process " + this.TTL);
        System.out.println("Total Line Limit for process " + this.TLL);
    }
}


class VirtualMachine {
    char[][] M;
    char[] IR;
    char[] R;
    int PTR;
    PCB process;
    int VA;
    int RA;
    int TTC;
    int TLC;
    int PI;
    int TI;
    int IC;
    int SI;
    boolean C;

    VirtualMachine() {
        this.M = new char[300][4];
        this.IR = new char[4];
        this.R = new char[4];
        this.PTR = 0;
        this.process = new PCB();
        this.VA = 0;
        this.RA = 0;
        this.TTC = 0;
        this.TLC = 0;
        this.PI = 0;
        this.TI = 0;
        this.IC = 0;
        this.C = false;
        this.SI = 0;
    }

    void clear(UserMode user) {
        this.M = new char[300][4];
        this.IR = new char[4];
        this.R = new char[4];
        this.PTR = 0;
        this.process = new PCB();
        this.VA = 0;
        this.RA = 0;
        this.TTC = 0;
        this.TLC = 0;
        this.PI = 0;
        this.TI = 0;
        this.IC = 0;
        this.C = false;
        this.SI = 0;
        user.hash = new HashMap<>();
    }

    boolean MOS(UserMode user) {

        if (this.SI == 1 && this.TI == 0) {
            if (!READ(user)) {
                return false;
            }
            this.SI = 0;
            return true;
        } else if (this.SI == 2 && this.TI == 0) {
            if (!WRITE(user)) {
                return false;
            }
            this.SI = 0;
            return true;
        } else if (this.SI == 3 && this.TI == 0) {
            TERMINATE(user, 0);
            return false;
        } else if (this.SI == 1 && this.TI == 2) { // Time limit exceeded
            TERMINATE(user, 3);
            return false;
        } else if (this.SI == 2 && this.TI == 2) {
            if (!WRITE(user)) {
                return false;
            }
            TERMINATE(user, 3);
            return false;
        } else if (this.SI == 3 && this.TI == 2) {
            TERMINATE(user, 0);
        } else if (this.PI == 1 && this.TI == 0) { //opcode error
            TERMINATE(user, 4);
            return false;
        } else if (this.PI == 2 && this.TI == 0) {//operand error
            TERMINATE(user, 5);
            return false;
        } else if (this.PI == 3 && this.TI == 0) { //page fault

            String opcode = String.valueOf(this.IR).trim().substring(0, 2);

            if (opcode.equals("GD") || opcode.equals("SR")) { //valid page fault

                int frameNo = user.allocate();
                user.addPageTableEntry(this, frameNo, this.VA / 10);
                this.RA = frameNo;
                this.PI = 0;
                this.IC--; //adjust IC and TTC
                //this.TTC--;

                return true;
            } else { //invaild pagefault

                TERMINATE(user, 6);
                return false;
            }
        } else if (this.PI == 1 && this.TI == 2) {
            TERMINATE(user, 7);
            return false;
        } else if (this.PI == 2 && this.TI == 2) {
            TERMINATE(user, 8);
            return false;
        } else if (this.PI == 3 && this.TI == 2) {
            TERMINATE(user, 3);
            return false;
        } else if (this.TI == 2) {
            TERMINATE(user, 3);
            return false;
        }

        return true;


    }


    boolean READ(UserMode user) {

        int block = this.RA;
        int word = 0;

        try {

            user.buffer = user.reader.readLine();

            if (user.buffer.substring(0, Math.min(4, user.buffer.length())).trim().equals("$END")) {
                TERMINATE(user, 1);
                return false;
            }
            for (char c : user.buffer.toCharArray()) {
                if (word == 4) {
                    word = 0;
                    block++;
                }
                this.M[block][word++] = c;
            }

        } catch (IOException exp) {
            System.out.println(exp);
        }
        return true;
    }

    boolean WRITE(UserMode user) {

        this.TLC++;
        if (this.TLC > this.process.TLL) {
            TERMINATE(user, 2);
            return false;
        }
        int block = this.RA;

        int length = block + 10;
        user.buffer = "";
        boolean flag = true;
        for (int i = block; i < length; i++) {
            for (int j = 0; j < 4; j++) {
                if (this.M[i][j] == '\0') {
                    flag = false;
                    break;
                }
                user.buffer += Character.toString(M[i][j]);
            }

        }

        try {

            user.writer.write(user.buffer);
            user.writer.newLine();
            user.writer.flush();

        } catch (IOException exp) {
            System.out.println(exp);
        }

        return true;
    }

    void TERMINATE(UserMode user, int error) {

        String status = "JID: " + this.process.job_id + " IC: " + this.IC + " IR: " + String.valueOf(this.IR) + " TTL: " + this.process.TTL + " TLL: " + this.process.TLL + " TTC: " + this.TTC + " TLC: " + this.TLC + " SI: " + this.SI + " TI: " + this.TI + " PI: " + this.PI + "\n";

        switch (error) {
            case 0:
                status += "Error status 0: No error\n";
                break;
            case 1:
                status += "Error status 1: Out of Data\n";
                break;
            case 2:
                status += "Error status 2: Line limit exceeded\n";
                break;
            case 3:
                status += "Error status 3: Time limit  exceeded\n";
                break;
            case 4:
                status += "Error status 4: Operation Code\n";
                break;
            case 5:
                status += "Error status 5: Operand Error\n";
                break;
            case 6:
                status += "Error status 6: Invalid Page Fault\n";
                break;
            case 7:
                status += "Error status 3: Time limit  exceeded\n";
                status += "Error status 4: Operation Code\n";
                break;
            case 8:
                status += "Error status 3: Time limit  exceeded\n";
                status += "Error status 5: Operand Error\n";
                break;
        }

        try {
            user.writer.write(status);
            user.writer.newLine();
            user.writer.newLine();
            user.writer.flush();
        } catch (IOException exp) {
            System.out.println(exp);
        }
    }


}


class UserMode {

    BufferedReader reader;
    BufferedWriter writer;
    String buffer;
    Random r;
    HashMap<Integer, Integer> hash;

    UserMode(String input, String output) {

        this.r = new Random();
        this.hash = new HashMap<>();
        try {
            String firstHalfOfThePath = "D:\\Multiprogramming-OS-main\\Multiprogramming-OS-main\\";
            this.reader = new BufferedReader(new FileReader(firstHalfOfThePath+input));
            this.writer = new BufferedWriter(new FileWriter(output));
            buffer = "";

        } catch (IOException exp) {
            System.out.println("Error while initiating: " + exp);
        }

    }

    int allocate() {


        int number = 0;

        while (hash.get((number = r.nextInt(30))) != null) ;
        hash.put(number, number);

        return number;

    }

    void initialisePageTable(VirtualMachine vm) {

        for (int i = vm.PTR * 10; i < vm.PTR * 10 + 10; i++) {

            vm.M[i][0] = '0';
            vm.M[i][1] = '0';
            vm.M[i][2] = '*';
            vm.M[i][3] = '*';

        }
    }

    void addPageTableEntry(VirtualMachine vm, int frameNo, int counter) {
        System.out.println("Adding frame no " + frameNo + " into page Table at " + counter + " " + vm.PTR);
        vm.M[vm.PTR * 10 + counter][0] = '1';
        vm.M[vm.PTR * 10 + counter][1] = '1';
        vm.M[vm.PTR * 10 + counter][2] = (char) (frameNo / 10 + 48);
        vm.M[vm.PTR * 10 + counter][3] = (char) (frameNo % 10 + 48);
    }

    boolean load(VirtualMachine vm) {
        int counter = 0;
        int word = 0;
        boolean newProgram = true;
        try {
            while ((this.buffer = this.reader.readLine()) != null) {

                System.out.println(buffer + "input " + newProgram);

                String card = (this.buffer.charAt(0) == 'H') ? "H" : this.buffer.substring(0, Math.min(buffer.length(), 4));

                if (!newProgram) {
                    if (card.equals("$AMJ")) {
                        newProgram = true;
                        word = 0;

                        counter = 0;
                    } else {
                        System.out.println("continue");
                        continue;
                    }
                }

                switch (card) {

                    case "$AMJ":

                        vm.clear(this);
                        vm.process.initialise(this.buffer);
                        vm.process.details();
                        vm.PTR = allocate();
                        System.out.println("Page Table allocated in block " + vm.PTR);
                        initialisePageTable(vm);
                        break;

                    case "$DTA":

                        word = 0;
                        for (int i = 0; i < 300; i++) {
                            System.out.println("M[" + i + "]\t" + vm.M[i][0] + "\t" + vm.M[i][1] + "\t" + vm.M[i][2] + "\t" + vm.M[i][3]);
                        }

                        startExecution(vm);
                        newProgram = false;
                        System.out.println("Program ended " + newProgram);
                        break;

                    case "$END":
                        System.out.println("Job completed with contents of memory as ");
                        for (int i = 0; i < 300; i++) {
                            System.out.println("M[" + i + "]\t" + vm.M[i][0] + "\t" + vm.M[i][1] + "\t" + vm.M[i][2] + "\t" + vm.M[i][3]);
                        }
                        word = 0;
                        counter = 0;
                        break;

                    default:
                        word = 0;
                        if (counter == 10) {
                            System.out.println("Error !! Memory exceeded ");
                            return false;
                        }
                        //alloacte frame for page
                        int frameNo = allocate();
                        //add frame no into page table
                        addPageTableEntry(vm, frameNo, counter);
                        frameNo *= 10;
                        for (char c : buffer.toCharArray()) {
                            if (word == 4) {
                                word = 0;
                                frameNo++;
                            }
                            vm.M[frameNo][word++] = c;
                        }

                        counter++;
                }
            }
        } catch (IOException exp) {
            System.out.println(exp);
        }
        for (int i = 0; i < 300; i++) {
            System.out.println("M[" + i + "]\t" + vm.M[i][0] + "\t" + vm.M[i][1] + "\t" + vm.M[i][2] + "\t" + vm.M[i][3]);
        }
        return true;
    }

    void startExecution(VirtualMachine vm) {
        vm.IC = 0;
        ExecuteUserProgram(vm);
    }

    void addressMap(VirtualMachine vm, String VA) {

        System.out.println("Ini VA " + VA);

        //if(VA.charAt(0) != 'H'){

        //String operand = String.valueOf(VA).substring(2,4);
        if (this.isNumeric(VA)) {

            if (Integer.parseInt(VA) < 0 || Integer.parseInt(VA) > 100) {
                vm.PI = 2;
            } else {
                System.out.println("operand " + VA);
                vm.VA = Integer.parseInt(VA);
            }

        } else {
            vm.PI = 2;

        }

        //}


        if (vm.M[vm.PTR * 10 + vm.VA / 10][0] != '0') {
            int frameNo = Character.getNumericValue(vm.M[vm.PTR * 10 + vm.VA / 10][2]) * 10 + Character.getNumericValue(vm.M[vm.PTR * 10 + vm.VA / 10][3]);
            vm.RA = frameNo * 10 + vm.VA % 10;

        } else {
            vm.PI = 3; //page fault
        }

        System.out.println("VA " + vm.VA + " RA " + vm.RA);

    }

    boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    void simulate(VirtualMachine vm) {
        vm.TTC++;
        System.out.println("TLC " + vm.TTC);
        if (vm.TTC > vm.process.TTL) {
            vm.TI = 2;
        }
    }

    void ExecuteUserProgram(VirtualMachine vm) {

        boolean end = false;
        boolean ahead = true;
        while (!end) {

            //vm.VA = vm.IC;
            addressMap(vm, String.valueOf(vm.IC));

            if (vm.PI != 0) { //error
                //if(!vm.MOS(this)){  
                //break;
                ahead = false;
                //};

            }

            if (ahead) {

                for (int i = 0; i < 4; i++) {
                    vm.IR[i] = vm.M[vm.RA][i];
                }
                vm.IC++;

                if (vm.IR[0] != 'H') addressMap(vm, String.valueOf(Arrays.copyOfRange(vm.IR, 2, 4)));
                System.out.println(String.valueOf(vm.IR));


                if (vm.PI != 0) {
                    ahead = false;


                    if (vm.PI == 3 && !vm.MOS(this)) {
                        end = true;
                        System.out.println("Some error");
                        break;
                    } else {
                        System.out.println("valid pf");
                        ahead = true;
                        continue;
                    }
                }
            }

            if (ahead) {
                String instruction = String.valueOf(vm.IR);
                String operation = instruction.substring(0, Math.min(2, instruction.length()));
                //System.out.println(String.valueOf(vm.IR));
                int block = 0;

                switch (operation.trim()) {
                    case "GD":
                        vm.SI = 1;
                        /*if(!vm.MOS(this)){  
                            end = true;                       
                        };*/
                        break;

                    case "PD":
                        vm.SI = 2;
                        /*if(!vm.MOS(this)){                        
                            end = true;
                        };*/
                        break;

                    case "H":
                        vm.SI = 3;
                        //vm.MOS(this);
                        //end = true;
                        break;

                    case "LR":


                        for (int i = 0; i < 4; i++) {
                            vm.R[i] = vm.M[vm.RA][i];
                        }
                        break;
                    case "SR":


                        for (int i = 0; i < 4; i++) {
                            vm.M[vm.RA][i] = vm.R[i];
                        }
                        break;

                    case "CR":

                        vm.C = true;
                        for (int i = 0; i < 4; i++) {
                            if (vm.R[i] != vm.M[vm.RA][i]) {
                                vm.C = false;
                                break;
                            }
                        }
                        break;

                    case "BT":
                        if (vm.C) {
                            block = Character.getNumericValue(vm.IR[2]);
                            block = block * 10 + Character.getNumericValue(vm.IR[3]);
                            vm.IC = block;
                        }
                        break;
                    default:
                        vm.PI = 1;
                        /*if(!vm.MOS(this)){
                            
                            end = true;    
                        }*/
                }

            }
            simulate(vm);
            if (vm.TI != 0 || vm.PI != 0 || vm.SI != 0) {
                if (!vm.MOS(this)) {
                    end = true;
                } else {
                    ahead = true;

                }

            }


        }

    }
}