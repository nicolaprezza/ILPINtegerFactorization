# ILPINtegerFactorization
Integer Factorization using Integer Linear Programming (CPLEX)

Factoring an integer is a hard task! This ILP model approaches
the problem by simulating binary multiplication with a
binary matrix. Even if in my model the number of
variables is quadratic with respect to the number of bits
of the integer to be factorized, only a linear number of
variables is forced to be integer. Anyway, this doesn't
seem to be enough to solve the problem in reasonable
time:)
