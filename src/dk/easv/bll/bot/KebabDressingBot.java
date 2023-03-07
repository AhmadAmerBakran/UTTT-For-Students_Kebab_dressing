package dk.easv.bll.bot;

import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

public class KebabDressingBot implements IBot{

    private static final String BOTNAME = "Kebab Dressings Bot";



    @Override
    public String getBotName() {
        return BOTNAME;
    }


    @Override
    public IMove doMove(IGameState state) {
        //TO DO Implementation of this AI's bot, look at the other bots so you know how to build your own strategy.
        return null;
    }

}
