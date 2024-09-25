package santomon.ImpossibleGame.phaseecho;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhaseEcho extends BaseShipSystemScript {


    private ShipAPI anchorMarker;
    private Float anchorAngularVelocity;
    private Vector2f anchorVelocity = new Vector2f();
    private AfterImageEffect afterImageEffect;
    private AnchorEffect anchorEffect;


    private final Vector2f shipLocationBeforeEcho = new Vector2f();
    private Float shipFacingBeforeEcho;

    private String id;
    private static final float MANEUVERABILITY_BOOST = 0.5f;
    private static final float TOP_SPEED_BOOST = 80F;  // 30 more than

    private static final float AFTER_IMAGE_DURATION = .5f;
    private static final float AFTER_IMAGE_INTERVAL = 0.5f;


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        this.id = id;

        if (!hasSpawnedAnchorMarker()) {
            createAnchorMarker(ship);
        }


        if (state == State.IN || state == State.ACTIVE) {
            // copied from maneuvering jets
            if (this.anchorEffect == null) {
                this.anchorEffect = new AnchorEffect(ship);
                Global.getCombatEngine().addPlugin(anchorEffect);
            }
            this.anchorEffect.setActive(true);


            stats.getMaxSpeed().modifyFlat(id, TOP_SPEED_BOOST);
            stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getDeceleration().modifyPercent(id, 200f * effectLevel);
            stats.getTurnAcceleration().modifyFlat(id, 30f * effectLevel);
            stats.getTurnAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getMaxTurnRate().modifyFlat(id, 15f);
            stats.getMaxTurnRate().modifyPercent(id, 100f);

            this.shipLocationBeforeEcho.set(ship.getLocation());
            this.shipFacingBeforeEcho = ship.getFacing();
        }


        if (state == State.OUT) {
//            if (!hasCreatedAfterImageEffect()) createAfterImageEffect(ship);
//            this.afterImageEffect.setActive(true);
            // copied from maneuvering jets
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);

            // displace back. using this implementation it accelerates towards the anchor position
            // could also do linear 🤔
            ship.setPhased(true);
            ship.getVelocity().set(0, 0);  // should we set to 0?
//            ship.getLocation().set(
//                    this.shipLocationBeforeEcho.x * effectLevel + anchorMarker.getLocation().x * (1 - effectLevel),
//                    this.shipLocationBeforeEcho.y * effectLevel + anchorMarker.getLocation().y * (1 - effectLevel)
//            );
//
//            ship.setFacing(this.shipFacingBeforeEcho * effectLevel + anchorMarker.getFacing() * (1 - effectLevel));

        }


    }

    private void createAfterImageEffect(ShipAPI ship) {
        ShipSystemAPI shipSystemAPI = ship.getSystem();
        this.afterImageEffect = new AfterImageEffect(
                ship,
                AFTER_IMAGE_DURATION,
                AFTER_IMAGE_INTERVAL
        );
        Global.getCombatEngine().addPlugin(afterImageEffect);
    }

    private void createAnchorMarker(ShipAPI ship) {
//        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
//        ShipVariantAPI anchorVariant = ship.getVariant().clone();
//        ShipHullSpecAPI shipHullSpecAPI = Global.getSettings().getHullSpec(ship.getHullSpec().getHullId());
//        anchorVariant.setHullSpecAPI(shipHullSpecAPI);
//
//        FleetMemberAPI memberAPI = Global.getFactory().createFleetMember(
//                FleetMemberType.SHIP,
//                anchorVariant
//        );
//
//
//        memberAPI.setOwner(ship.getOwner());
//        CombatFleetManagerAPI combatFleetManagerAPI = combatEngineAPI.getFleetManager(ship.getOwner());
//        combatFleetManagerAPI.setSuppressDeploymentMessages(true);
//        ShipAPI anchor = combatFleetManagerAPI.spawnFleetMember(memberAPI,
//                ship.getLocation(),
//                ship.getFacing(),
//                0f
//        );
//        combatFleetManagerAPI.setSuppressDeploymentMessages(false);
//
//        combatFleetManagerAPI.removeDeployed(anchor, true);
//        combatEngineAPI.addEntity(anchor);
//
//        anchor.makeLookDisabled();
//        anchor.setPhased(true);


        this.anchorMarker = null;
        this.anchorAngularVelocity = ship.getAngularVelocity();
        this.anchorVelocity = this.anchorVelocity.set(ship.getVelocity());

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

        if (this.anchorEffect != null) {
            this.anchorEffect.setActive(false);
        }

        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);

        ship.getVelocity().set(this.anchorVelocity);

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();
        if (this.anchorMarker != null) {
            combatEngineAPI.removeEntity(this.anchorMarker);
            this.anchorMarker = null;
        }

        if (this.anchorAngularVelocity != null) {
            ship.setAngularVelocity(this.anchorAngularVelocity);
            this.anchorAngularVelocity = null;
        }
        ship.getVelocity().set(this.anchorVelocity);
    }


}


class AfterImageEffect extends BaseEveryFrameCombatPlugin {
    private final ShipAPI ship;
    private final Logger log = Global.getLogger(AfterImageEffect.class);

    private float timeSinceLastAfterImageSpawned = 0;
    private float timeElapsedSinceInstantiation = 0;

    private float afterImageDuration = 0.5f;
    private float afterImageInterval = 0.5f;
    private boolean active = false;

    private List<TimestampedData> timestampedDataList = new ArrayList<>();
    private List<SpriteData> toRender = new ArrayList<>();

    AfterImageEffect(ShipAPI ship, Float afterImageDuration, Float afterImageInterval) {
        this.ship = ship;
        String spriteName = ship.getHullSpec().getSpriteName();
        try {
            Global.getSettings().loadTexture(spriteName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    public void advance(float amount, List<InputEventAPI> events) {
        super.advance(amount, events);

        // not sure how this after image thing behaves with time acceleration?
        timeElapsedSinceInstantiation += amount;
        timeSinceLastAfterImageSpawned += amount;
        if (!this.active) return;
        if (this.ship == null) return;

        Vector2f location = new Vector2f();
        location.set(ship.getLocation().x, ship.getLocation().y);
        timestampedDataList.add(new TimestampedData(timeElapsedSinceInstantiation, location, ship.getFacing(), afterImageDuration));


        if (timeSinceLastAfterImageSpawned > afterImageInterval) {
            timeSinceLastAfterImageSpawned -= afterImageInterval;
            addToRender();
            pruneTimestampedData();
        }
        pruneToRender();
    }

    private void adjustRenderLifetime(float amount) {

    }

    private void pruneToRender() {
        List<SpriteData> prunedToRender = new ArrayList<>();
        for ( SpriteData spriteDataElement : this.toRender) {
            if (spriteDataElement.remainingLifeTime > 0) {
                prunedToRender.add(spriteDataElement);
            }
        }
        this.toRender = prunedToRender;
    }


    private void addToRender() {
        for (TimestampedData timestampedDataElement : this.timestampedDataList) {
            if (this.timeElapsedSinceInstantiation - timestampedDataElement.timestamp < afterImageInterval) {
                log.info(this.toRender);

                SpriteAPI sprite = Global.getSettings().getSprite(this.ship.getHullSpec().getSpriteName());
                Vector2f location = ship.getLocation();
                sprite.setAngle(ship.getFacing() - 90f);
                this.toRender.add(new SpriteData(sprite, location, afterImageDuration));
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

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        super.renderInWorldCoords(viewport);

        for (SpriteData spriteData : this.toRender) {
            spriteData.remainingLifeTime -= Global.getCombatEngine().getTotalElapsedTime(false);
            log.info("rendering: " + spriteData + " " + spriteData.location);
            spriteData.spriteAPI.renderAtCenter(spriteData.location.x, spriteData.location.y);
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


class SpriteData {

    public final SpriteAPI spriteAPI;
    public final Vector2f location;
    public float remainingLifeTime;

    SpriteData(SpriteAPI spriteAPI, Vector2f location,  float lifeTime) {
        this.spriteAPI = spriteAPI;
        this.remainingLifeTime = lifeTime;
        this.location = location;
    }

}