package santomon.ImpossibleGame;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class PhaseEcho  extends BaseShipSystemScript {


    private ShipAPI anchorMarker;

    private String id;
    private static final float MANEUVERABILITY_BOOST = 0.5f;
    private static final float TOP_SPEED_BOOST = 80F;  // 30 more than


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        this.id = id;

        if (!hasSpawnedAnchorMarker()) {
            createAnchorMarker(ship);
        }


        if (state == State.IN || state == State.ACTIVE) {
            // copied from maneuvering jets
            stats.getMaxSpeed().modifyFlat(id, TOP_SPEED_BOOST);
            stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getDeceleration().modifyPercent(id, 200f * effectLevel);
            stats.getTurnAcceleration().modifyFlat(id, 30f * effectLevel);
            stats.getTurnAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getMaxTurnRate().modifyFlat(id, 15f);
            stats.getMaxTurnRate().modifyPercent(id, 100f);
        }


        if (state == State.OUT) {
            // copied from maneuvering jets
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);

            // displace back. using this implementation it accelerates towards the anchor position
            // could also do linear 🤔
            ship.setPhased(true);
            ship.getVelocity().set(0, 0);  // should we set to 0?
            ship.getLocation().set(
                    ship.getLocation().x * effectLevel + anchorMarker.getLocation().x * (1 - effectLevel),
                    ship.getLocation().y * effectLevel + anchorMarker.getLocation().y * (1 - effectLevel)
            );

            ship.setFacing(ship.getFacing() * effectLevel + anchorMarker.getFacing() * ( 1- effectLevel));

        }






    }

    private void createAnchorMarker(ShipAPI ship) {
        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        ShipVariantAPI anchorVariant = ship.getVariant().clone();
        ShipHullSpecAPI shipHullSpecAPI = Global.getSettings().getHullSpec(ship.getHullSpec().getHullId());
        anchorVariant.setHullSpecAPI(shipHullSpecAPI);

        FleetMemberAPI memberAPI = Global.getFactory().createFleetMember(
                FleetMemberType.SHIP,
                anchorVariant
        );


        memberAPI.setOwner(ship.getOwner());
        CombatFleetManagerAPI combatFleetManagerAPI = combatEngineAPI.getFleetManager(ship.getOwner());
        combatFleetManagerAPI.setSuppressDeploymentMessages(true);
        ShipAPI anchor = combatFleetManagerAPI.spawnFleetMember(memberAPI,
                ship.getLocation(),
                ship.getFacing(),
                0f
                );
        combatFleetManagerAPI.setSuppressDeploymentMessages(false);

        combatFleetManagerAPI.removeDeployed(anchor, true);
        combatEngineAPI.addEntity(anchor);

        anchor.makeLookDisabled();
        anchor.setPhased(true);



        this.anchorMarker = anchor;

    }

    private boolean hasSpawnedAnchorMarker() {
        return this.anchorMarker != null;

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        ship.setPhased(false);

        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        if (this.anchorMarker != null) {
            combatEngineAPI.removeEntity(this.anchorMarker);
            this.anchorMarker = null;
        }

    }



}
