# Sudoku CSP Application
Sudoku solver bot using the apporach of Constraint Satisfaction Problems

## Variables
81 Variables, each one representing the value of a cell in the board
V_ij, where i = row number and j = column number --> V_11,V_12,V_13,… ,V_98,V_99

## Domains
V_ij∈{1,2,3,4,5,6,7,8,9}  for initially empty cells

## Constraints
No cell in the same column can have the same value.
No cell in the same row can have the same value.
No cell in the same sub-square can have the same value.
  
### For rows
Alldiff(Vr1, Vr2, Vr3, Vr4, Vr5, Vr6, Vr7, Vr8, Vr9) where r ∈[1,9] (9 constraints)
### For colums
Alldiff(V1c, V2c, V3c, V4c, V5c, V6c, V7c, V8c, V9c) where c ∈[1,9] (9 constraints)
### For sub-squares 
Alldiff(V11, V12, V13, V21, V22, V23, V31, V32, V33)
Alldiff(V41, V42, V43, V51, V52, V53, V61, V62, V63)
Alldiff(V71, V72, V73, V81, V82, V83, V91, V92, V93)
Alldiff(V14, V15, V16, V24, V25, V26, V34, V35, V36)
Alldiff(V44, V45, V46, V54, V55, V56, V64, V65, V66)
Alldiff(V74, V75, V76, V84, V85, V86, V94, V95, V96)
