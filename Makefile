default:
	java -ea -cp rats.jar xtc.parser.Rats -lgpl Tack.rats
	javac -g -cp rats.jar -Xlint:unchecked *.java

clean:
	rm TackParser.java *.class *.ast

cleanall:
	rm TackParser.java *.class *.ast *.ir *.s *.exe *.*~
run0:
	java -ea -cp .:rats.jar Main test/000.tack
run1:
	java -ea -cp .:rats.jar Main test/001.tack
run2:
	java -ea -cp .:rats.jar Main test/002.tack
run3:
	java -ea -cp .:rats.jar Main test/003.tack
run4:
	java -ea -cp .:rats.jar Main test/004.tack
run5:
	java -ea -cp .:rats.jar Main test/005.tack
run6:
	java -ea -cp .:rats.jar Main test/006.tack
run7:
	java -ea -cp .:rats.jar Main test/007.tack
run8:
	java -ea -cp .:rats.jar Main test/008.tack
run9:
	java -ea -cp .:rats.jar Main test/009.tack
run10:
	java -ea -cp .:rats.jar Main test/010.tack
run11:
	java -ea -cp .:rats.jar Main test/011.tack
run18:
	java -ea -cp .:rats.jar Main test/018.tack
run19:
	java -ea -cp .:rats.jar Main test/019.tack
run20:
	java -ea -cp .:rats.jar Main test/020.tack
run22:
	java -ea -cp .:rats.jar Main test/022.tack
irall:
	java -ea -cp .:rats.jar Main test/001.tack > 001.ir
	java -ea -cp .:rats.jar Main test/002.tack > 002.ir
	java -ea -cp .:rats.jar Main test/003.tack > 003.ir
	java -ea -cp .:rats.jar Main test/004.tack > 004.ir
	java -ea -cp .:rats.jar Main test/005.tack > 005.ir
	java -ea -cp .:rats.jar Main test/006.tack > 006.ir
	java -ea -cp .:rats.jar Main test/007.tack > 007.ir
	java -ea -cp .:rats.jar Main test/008.tack > 008.ir
	java -ea -cp .:rats.jar Main test/009.tack > 009.ir
	java -ea -cp .:rats.jar Main test/010.tack > 010.ir
	java -ea -cp .:rats.jar Main test/011.tack > 011.ir
	java -ea -cp .:rats.jar Main test/018.tack > 018.ir
	java -ea -cp .:rats.jar Main test/019.tack > 019.ir
	java -ea -cp .:rats.jar Main test/020.tack > 020.ir
	java -ea -cp .:rats.jar Main test/022.tack > 022.ir
ipall:
	java -ea -jar IRInterpreter.jar 001.ir
	java -ea -jar IRInterpreter.jar 002.ir
	java -ea -jar IRInterpreter.jar 003.ir
	java -ea -jar IRInterpreter.jar 004.ir
	java -ea -jar IRInterpreter.jar 005.ir
	java -ea -jar IRInterpreter.jar 006.ir
	java -ea -jar IRInterpreter.jar 007.ir
	java -ea -jar IRInterpreter.jar 008.ir
	java -ea -jar IRInterpreter.jar 009.ir
	java -ea -jar IRInterpreter.jar 010.ir
	java -ea -jar IRInterpreter.jar 011.ir
	java -ea -jar IRInterpreter.jar 018.ir
	java -ea -jar IRInterpreter.jar 019.ir
	java -ea -jar IRInterpreter.jar 020.ir
	java -ea -jar IRInterpreter.jar 022.ir
bsall:
	java -ea -cp rats.jar xtc.parser.Rats -lgpl Tack.rats
	javac -g -cp rats.jar -Xlint:unchecked *.java
	java -ea -cp .:rats.jar Main test/001.tack > 001.s
	java -ea -cp .:rats.jar Main test/002.tack > 002.s
	java -ea -cp .:rats.jar Main test/003.tack > 003.s
	java -ea -cp .:rats.jar Main test/004.tack > 004.s
	java -ea -cp .:rats.jar Main test/005.tack > 005.s
	java -ea -cp .:rats.jar Main test/006.tack > 006.s
	java -ea -cp .:rats.jar Main test/007.tack > 007.s
	java -ea -cp .:rats.jar Main test/008.tack > 008.s
	java -ea -cp .:rats.jar Main test/009.tack > 009.s
	java -ea -cp .:rats.jar Main test/010.tack > 010.s
	java -ea -cp .:rats.jar Main test/011.tack > 011.s
	java -ea -cp .:rats.jar Main test/018.tack > 018.s
	java -ea -cp .:rats.jar Main test/019.tack > 019.s
	java -ea -cp .:rats.jar Main test/020.tack > 020.s
	java -ea -cp .:rats.jar Main test/022.tack > 022.s

exeall:
	gcc -m64 -masm=intel -o 001.exe 001.s x64runtime.c 
	gcc -m64 -masm=intel -o 002.exe 002.s x64runtime.c 
	gcc -m64 -masm=intel -o 003.exe 003.s x64runtime.c 
	gcc -m64 -masm=intel -o 004.exe 004.s x64runtime.c 
	gcc -m64 -masm=intel -o 005.exe 005.s x64runtime.c 
	gcc -m64 -masm=intel -o 006.exe 006.s x64runtime.c 
	gcc -m64 -masm=intel -o 007.exe 007.s x64runtime.c 
	gcc -m64 -masm=intel -o 008.exe 008.s x64runtime.c 
	gcc -m64 -masm=intel -o 009.exe 009.s x64runtime.c 
	gcc -m64 -masm=intel -o 010.exe 010.s x64runtime.c 
	gcc -m64 -masm=intel -o 011.exe 011.s x64runtime.c 
	gcc -m64 -masm=intel -o 018.exe 018.s x64runtime.c 
	gcc -m64 -masm=intel -o 019.exe 019.s x64runtime.c 
	gcc -m64 -masm=intel -o 020.exe 020.s x64runtime.c 
	gcc -m64 -masm=intel -o 022.exe 022.s x64runtime.c 
runall:
	./001.exe
	./002.exe
	./003.exe
	./004.exe
	./005.exe
	./006.exe
	./007.exe
	./008.exe
	./009.exe
	./010.exe
	./011.exe
	./018.exe
	./019.exe
	./020.exe
	./022.exe
