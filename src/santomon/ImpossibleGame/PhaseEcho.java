package santomon.ImpossibleGame;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class PhaseEcho  extends BaseShipSystemScript {


    private ShipAPI anchorMarker;
    private AfterImageEffect afterImageEffect;

    private String id;
    private static final float MANEUVERABILITY_BOOST = 0.5f;
    private static final float TOP_SPEED_BOOST = 80F;  // 30 more than

    private static final float AFTER_IMAGE_DURATION = 0.5f;
    private static final float AFTER_IMAGE_INTERVAL = 0.2f;


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
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
            if (!hasCreatedAfterImageEffect()) createAfterImageEffect(ship);
            this.afterImageEffect.setActive(true);
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

    private void createAfterImageEffect(ShipAPI ship) {
        ShipSystemAPI shipSystemAPI = ship.getSystem();
        this.afterImageEffect = new AfterImageEffect(
                ship,
                AFTER_IMAGE_DURATION,
                AFTER_IMAGE_INTERVAL
        );


        if (!ship.hasListenerOfClass(AfterImageEffect.class)) {
            ship.addListener(this.afterImageEffect);
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

    private boolean hasCreatedAfterImageEffect() {
        return this.afterImageEffect != null;

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        ship.setPhased(false);

        if (this.afterImageEffect != null) {
            this.afterImageEffect.setActive(false);
        }

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


class AfterImageEffect implements AdvanceableListener {
    private final ShipAPI ship;
    private final Logger log = Global.getLogger(AfterImageEffect.class);

    private float timeSinceLastAfterImageSpawned = 0;
    private float timeElapsedSinceInstantiation = 0;

    private float afterImageDuration = 0.5f;
    private float afterImageInterval = 0.5f;
    private boolean active = false;

    private List<TimestampedData> timestampedDataList = new ArrayList<>();
    private List<TimestampedData> toRender = new ArrayList<>();

    AfterImageEffect(ShipAPI ship, Float afterImageDuration, Float afterImageInterval) {
        this.ship = ship;
        if (afterImageDuration != null) {
            this.afterImageDuration = afterImageDuration;
        }
        if (afterImageInterval != null) {
            this.afterImageInterval = afterImageInterval;
        }

    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void advance(float amount) {
        // not sure how this after image thing behaves with time acceleration?
        timeElapsedSinceInstantiation += amount;
        timeSinceLastAfterImageSpawned += amount;
        if (!this.active) return;
        if (this.ship == null) return;
        log.info("ship location: " + ship.getLocation());

        timestampedDataList.add(new TimestampedData(timeElapsedSinceInstantiation, new Vector2f().set(ship.getLocation()), ship.getFacing(), afterImageDuration));



        if (timeSinceLastAfterImageSpawned > afterImageInterval) {
            timeSinceLastAfterImageSpawned -= afterImageInterval;
            addToRender();
            pruneTimestampedData();
        }

        maybeRender(amount);
        pruneToRender();
    }

    private void pruneToRender() {
        List<TimestampedData> prunedToRender = new ArrayList<>();
        for (TimestampedData toRenderElement : this.toRender) {
            if (toRenderElement.remainingLifeTime > 0) {
                prunedToRender.add(toRenderElement);
            }
        }
        this.toRender = prunedToRender;
    }


    private void addToRender() {
        for (TimestampedData timestampedDataElement: this.timestampedDataList) {
            if (this.timeElapsedSinceInstantiation - timestampedDataElement.timestamp < afterImageInterval) {
                this.toRender.add(timestampedDataElement);
                break;
            }
        }
    }

    private void pruneTimestampedData() {
        List<TimestampedData> prunedData = new ArrayList<>();
        for (TimestampedData timestampedDataElement : this.timestampedDataList) {
            if (this.timeElapsedSinceInstantiation - timestampedDataElement.timestamp < afterImageInterval) {
                prunedData.add(timestampedDataElement);
            }
        }
        this.timestampedDataList = prunedData;

    }

    private void maybeRender(float amount) {

        for (TimestampedData timestampedData : this.toRender) {
            log.info("are we even rendering shit?");

            timestampedData.remainingLifeTime -= amount;
            String spriteName = ship.getHullSpec().getSpriteName();
            SpriteAPI spriteAPI = Global.getSettings().getSprite(spriteName);  // hopefully this creates a fresh object
            spriteAPI.setAngle(timestampedData.facing - 90f);
            spriteAPI.renderAtCenter(0, 0);
//            spriteAPI.renderAtCenter(timestampedData.location.x, timestampedData.location.y);
        }

    }
}


class TimestampedData {

        public final Vector2f location;
        public final float facing;
        public final float timestamp;
        public float remainingLifeTime;

        TimestampedData(float timestamp, Vector2f location, float facing, float lifeTime) {
            this.facing = facing;
            this.timestamp = timestamp;
            this.location = location;
            this.remainingLifeTime = lifeTime;
        }
        }