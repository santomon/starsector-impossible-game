package santomon.ImpossibleGame.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class StarboardAssault extends BaseHullMod {
    public static final String tag = "impossible_starboard_assault";




    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {

    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
//        ship.addTag(tag);
//        for (WeaponAPI weaponAPI : ship.getAllWeapons()) {
//             would love to limit range of weapons on specific side 🤔
//
//        }
    }

}
