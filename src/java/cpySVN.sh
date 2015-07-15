#!/bin/bash
dest=$1
#A=`ls */* | fgrep ':' | sed 's/://g' | paste -s -` 
#B=`ls -l | egrep '^d' | sed 's/  */ /g' | cut -d ' ' -f 9 | paste -s -`
#D=`ls */*/* | fgrep ':' | sed 's/://g' | paste -s -` 
#E=`ls net/ddp2p/*/*/* | fgrep ':' | sed 's/://g' | paste -s -`
#F=`ls net/ddp2p/*/* | fgrep ':' | sed 's/://g' | paste -s -`

A=`find ./net/ -name '*.java' | sed 's-[^/]*$--g' | sort -u | paste -s -`
B=`echo -n tools/ util/db/ util/tools/ widgets/app/`
D=`echo -n ./ ./scripts/ ./scripts/handling_DEBUG/`
#E=`find ./test/ -name '*.java' | sed 's-[^/]*$--g' | sort -u | paste -s -`

C="./ $A $B "
#echo B=$B
#echo $C
cp createEmptyDelib.sqlite.comments $dest/
cp *.properties $dest/
for a in $A $B
do
 mkdir -p $dest/$a
 cp $a/*.java $dest/$a/
done

for a in $D
do
 mkdir -p $dest/$a
 cp $a/*.exe $dest/$a/
 cp $a/*.sh $dest/$a/
 cp $a/*.py $dest/$a/
 cp $a/*.bat $dest/$a/
 cp $a/*.c $dest/$a/
 cp $a/*.cpp $dest/$a/
 cp $a/Makefile $dest/$a/
 cp $a/*.sqlite $dest/$a/
done

for a in ./jars/
do
 mkdir -p $dest/$a
 cp $a/*.jar $dest/$a/
done

for a in p2pdd_resources p2pdd_resources/census  p2pdd_resources/steag
do
 mkdir -p $dest/$a
 cp $a/*.gif $dest/$a/
 cp $a/*.png $dest/$a/
 cp $a/*.jpg $dest/$a/
 cp $a/*.ico $dest/$a/
done

for a in plugins/chatApp/plugin /plugins/chatApp/chatApp
do
 mkdir -p $dest/$a
done

for a in plugins/chatApp/*/*java
do
 cp $a $dest/$a
done

#svn del widgets/org/FieldItem.java widgets/org/LVListener.java widgets/org/LVEditor.java widgets/org/LVComboBox.java widgets/motions/DocumentTitleRenderer.java widgets/motions/DocumentTitleRenderer.class handling_wb/BroadcastableMessages.java
