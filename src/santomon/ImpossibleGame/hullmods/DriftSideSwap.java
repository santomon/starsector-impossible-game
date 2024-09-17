package santomon.ImpossibleGame.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DriftSideSwap extends BaseHullMod {
    // modeled after armaa customization
    Logger log = Global.getLogger(DriftSideSwap.class);

    List<String> sides = new ArrayList<String>() {{
        add(StarboardAssault.tag);
        add(PortAssault.tag);
    }};

    String previouslySelected;


    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

        MutableShipStatsAPI stats = ship.getMutableStats();
//        log.info("previouslySelected: " + previouslySelected);

        // ensure we have a hullmod and a previouslySelected
        for (String side : sides) {
            if (stats.getVariant().getHullMods().contains(side)) {
                previouslySelected = side;
            }
        }
        if (previouslySelected == null) {
            // not sure when this will ever occur
            String side = sides.get(0);
            stats.getVariant().getHullMods().add(side);
            previouslySelected = side;
        }

        // if there is previouslySelected, but no accomodating hullmod, switch to the next
        if (!stats.getVariant().getHullMods().contains(previouslySelected)) {
//            log.info("swap check entered");
            int currentIndex = sides.indexOf(previouslySelected);
            int newIndex = (currentIndex + 1) % sides.size();
            String newSide = sides.get(newIndex);
            previouslySelected = newSide;
            stats.getVariant().getHullMods().add(newSide);
        }




    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {

    }




}
