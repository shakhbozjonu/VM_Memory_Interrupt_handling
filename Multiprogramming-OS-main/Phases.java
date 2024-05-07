
class Phases{

    public static void main(String[] args){
        int a;
        VirtualMachine vm = new VirtualMachine();
        String input="input_phase2.txt";
        String output="new_out_file_name.txt";

        UserMode usr = new UserMode(input,output);
        usr.load(vm);
    }
}
