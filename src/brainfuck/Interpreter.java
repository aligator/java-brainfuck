/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package brainfuck;

/**
 *
 * @author johannes
 */
public class Interpreter extends Thread{
   
    private final ConsoleFrame cf;
    private final char[] code; 
    private final boolean twoBytes;
    private int pointer;
    private int[] openBrackets;
    private int[] closedBrackets;
    private char input;
    private final Object sync = new Object();
    
    private final char[] array = new char[30000];
    
    public Interpreter(String input, boolean twoBytes, ConsoleFrame cf) {
        this.cf = cf;
        this.pointer = 0;
        this.code = input.toCharArray();
        this.twoBytes = twoBytes;
    }
        
    
    private void incrementData() {
        char value = array[pointer];
        if(twoBytes) {
            if(value < Character.MAX_VALUE)
                value += 1;
        } else {
            if(value < Byte.MAX_VALUE)
                value += 1;
        }
        array[pointer] = value;
    }
    
    private void decrementData() {
        char value = array[pointer];
        
        if(twoBytes) {
            if(value > Character.MIN_VALUE)
                value -= 1;
        } else {
            if(value > Byte.MIN_VALUE)
                value -= 1;
        }
        
        array[pointer] = value;
    }
    
    private void incrementPointer() {
        if(pointer < array.length - 1)
            pointer += 1;
    }
  
    private void decrementPointer() {
        if(pointer > 0)
            pointer -= 1;
    }
    
    private void output() {
        cf.addText((char)array[pointer]);
    }
    
    private char getChar() {
        return array[pointer];
    }
    
    private boolean getInput() {
        boolean kill = false;
        
        cf.requestInput();
        
        synchronized (this.sync) {
            try {
                this.sync.wait();
            } catch (InterruptedException ex) {
                kill = true;
            }
        }
        
        if(!kill) {
            if(!twoBytes && this.input > Byte.MAX_VALUE) {
                    array[pointer] = Byte.MAX_VALUE;
            } else {
                array[pointer] = this.input;
            }
        }
        return kill;
    }
    
    public void setInput(char input) {
        this.input = input; 
        
        synchronized(this.sync) {
            this.sync.notify();
        }
    }
    
    private void prepareCode() {
        int counter = 0;
                
        for(char chr : code) {
            if(chr == '[') {
                counter++;
            }
        }
        
        openBrackets = new int[counter];
        closedBrackets = new int[counter];
        
        counter = 0;
        int i = 0;
        for(char chr : code) {
            if(chr == '[') {
                openBrackets[counter] = i;
                int loops = 1;
                int loopPointer = i;
                
                while(loops > 0) {
                    loopPointer++;
                    if(code[loopPointer] == '[')
                        loops++;
                    if(code[loopPointer] == ']')
                        loops--;
                }
                closedBrackets[counter] = loopPointer;
                counter++;
            }
            i++;
        }
    }
    
    @Override
    public void run() {
        prepareCode();
        boolean kill = false;
        
        int i=0;
        while(i<code.length && !kill) {
            char chr = code[i];
            switch(chr) {
                case '>':
                    incrementPointer();
                    break;
                case '<':
                    decrementPointer();
                    break;
                case '+':
                    incrementData();
                    break;
                case '-':
                    decrementData();
                    break;
                case '.':
                    output();
                    break;
                case ',':
                    kill = getInput();
                    break;
                case '[':
                    if(getChar() == (char)0) {
                        int j = 0;
                        while(openBrackets[j] != i) {
                            j++;
                        }
                        i = closedBrackets[j];
                    }
                    break;
                case ']':
                    int j = 0;
                        while(closedBrackets[j] != i) {
                            j++;
                        }
                        i = openBrackets[j] - 1;
                    break;
                
            }
            i++;
        }
    }
}