# ExpressCogs

This project includes code for topological spiking neural networks. The code is based on jBLAS, a Java wrapper for the BLAS
linear algebra library, and JavaFX, a GUI toolkit.

To run simulations, clone the code using e.g. `git clone https://github.com/tim-shea/ExpressCogs` and import the project
into your java IDE. Classes in the `expresscogs.simulation` package can be run directly.

`TopologicalNetwork` is a simple network demonstrating the effect of topological connectivity on a standard
excitatory/inhibitory reservoir style network.

`SignalSelectionNetwork` is a spiking model of basal ganglia translated from Gurney, Prescott, and Redgrave (2001).
