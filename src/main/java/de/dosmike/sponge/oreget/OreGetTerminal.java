package de.dosmike.sponge.oreget;

import de.dosmike.sponge.oreget.decoupler.AbstractionProvider;
import de.dosmike.sponge.oreget.decoupler.standalone.Arguments;

public class OreGetTerminal {

    public static void main(String[] args_) {
        AbstractionProvider.get();

        Arguments args = new Arguments(args_);

        System.out.println("Done");
    }

}
