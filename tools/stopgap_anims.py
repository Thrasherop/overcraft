"""Run by Blender (headless) from build_asset_pack.py: adds placeholder animation clips to an imported hero rig (a Source
"ValveBiped" skeleton, as in SFM ports), until the hero's real clips can be extracted from the game (#6). Crude on
purpose: they exist to exercise the mod's animation system (a real crouch bends the knees; Steve bends at the waist).

Clips: idle, run, run_back, strafe_left, strafe_right, crouch_idle, crouch_walk, jump, fall, land, climb,
throw_kunai, throw_ofuda, throw_suzu, swift_step, ult_cast. Imported as a module by blender_to_glb.py.

Rotations are given about the character's own axes (she faces -Y in Blender after import, up is +Z): PITCH swings a
limb forward (-) or back (+), ROLL swings it out to the side, YAW twists. Each key is applied parent before child, so
a child turns with its parent and then by its own amount.
"""
import math

import bpy
from mathutils import Matrix, Quaternion, Vector

FPS = 30
B = "ValveBiped.Bip01_"

# her axes in Blender space: facing -Y, right hand at -X, up +Z
LATERAL = Vector((1, 0, 0))   # pitch axis
FORWARD = Vector((0, -1, 0))  # roll axis
UP = Vector((0, 0, 1))        # yaw axis


def pitch(deg):
    """Swing about the lateral axis: negative swings a hanging limb forward."""
    return Quaternion(LATERAL, math.radians(deg))


def roll(deg):
    """Swing about the forward axis: for the left arm, positive lowers it; for the right arm, negative lowers it."""
    return Quaternion(FORWARD, math.radians(-deg))


def yaw(deg):
    return Quaternion(UP, math.radians(deg))


# arms from the import's A-pose down to the sides
ARMS_DOWN = {"L_Upperarm": roll(40), "R_Upperarm": roll(-40)}


def pose(**bones):
    """A pose: bone short name -> world-axis rotation (or list of them, multiplied), plus 'drop' (pelvis down, m)."""
    out = dict(ARMS_DOWN)
    for k, v in bones.items():
        if k == "drop":
            out[k] = v
        elif k in out and isinstance(out[k], Quaternion):
            out[k] = v @ out[k]
        else:
            out[k] = v
    return out


CROUCH = dict(drop=0.42, Spine=pitch(-12), L_Thigh=pitch(-75), R_Thigh=pitch(-75), L_Calf=pitch(115),
              R_Calf=pitch(115), L_Foot=pitch(-40), R_Foot=pitch(-40))


def stride(phase, amount=40, knee=50, arm=30, crouch=False):
    s = math.sin(phase)
    base = dict(CROUCH) if crouch else {}
    lean = pitch(-8)
    legs = dict(
        L_Thigh=pitch(-amount * s) @ base.get("L_Thigh", Quaternion()),
        R_Thigh=pitch(amount * s) @ base.get("R_Thigh", Quaternion()),
        L_Calf=pitch(knee * max(0, s) + 10) @ base.get("L_Calf", Quaternion()),
        R_Calf=pitch(knee * max(0, -s) + 10) @ base.get("R_Calf", Quaternion()),
        L_Upperarm=pitch(arm * s),
        R_Upperarm=pitch(-arm * s),
        L_Forearm=pitch(-35),
        R_Forearm=pitch(-35),
        Spine=lean @ base.get("Spine", Quaternion()),
    )
    if crouch:
        legs["drop"] = CROUCH["drop"]
        legs["L_Foot"] = CROUCH["L_Foot"]
        legs["R_Foot"] = CROUCH["R_Foot"]
    else:
        legs["drop"] = 0.03 * abs(math.cos(phase))
    return pose(**legs)


def cycle(fn, frames, keys=8):
    return [(round(i * frames / keys), fn(2 * math.pi * i / keys)) for i in range(keys + 1)]


CLIPS = {
    "idle": [(0, pose(Spine=pitch(0))), (45, pose(Spine=pitch(-2), Spine2=pitch(-1))), (90, pose(Spine=pitch(0)))],
    "run": cycle(lambda p: stride(p), 20),
    "run_back": cycle(lambda p: stride(-p, amount=30), 22),
    "strafe_left": cycle(lambda p: pose(L_Thigh=roll(-25 * max(0, math.sin(p))), R_Thigh=roll(10 * max(0, -math.sin(p))),
                                         Spine=yaw(10)), 20),
    "strafe_right": cycle(lambda p: pose(R_Thigh=roll(25 * max(0, math.sin(p))), L_Thigh=roll(-10 * max(0, -math.sin(p))),
                                          Spine=yaw(-10)), 20),
    "crouch_idle": [(0, pose(**CROUCH)), (60, pose(**CROUCH))],
    "crouch_walk": cycle(lambda p: stride(p, amount=20, knee=20, arm=15, crouch=True), 28),
    "jump": [(0, pose(L_Thigh=pitch(-35), R_Thigh=pitch(-10), L_Calf=pitch(60), R_Calf=pitch(20),
                      L_Upperarm=roll(-20), R_Upperarm=roll(20))),
             (10, pose(L_Thigh=pitch(-45), R_Thigh=pitch(-20), L_Calf=pitch(80), R_Calf=pitch(40),
                       L_Upperarm=roll(-35), R_Upperarm=roll(35)))],
    "fall": [(0, pose(L_Thigh=pitch(-15), R_Thigh=pitch(10), L_Calf=pitch(25), R_Calf=pitch(15),
                      L_Upperarm=roll(-45), R_Upperarm=roll(45), Spine=pitch(5))),
             (20, pose(L_Thigh=pitch(-20), R_Thigh=pitch(15), L_Calf=pitch(30), R_Calf=pitch(10),
                       L_Upperarm=roll(-50), R_Upperarm=roll(50), Spine=pitch(5)))],
    "land": [(0, pose(drop=0.25, L_Thigh=pitch(-45), R_Thigh=pitch(-45), L_Calf=pitch(80), R_Calf=pitch(80),
                      L_Foot=pitch(-30), R_Foot=pitch(-30))), (6, pose())],
    "climb": cycle(lambda p: pose(
        L_Upperarm=roll(-150 + 25 * math.sin(p)) @ pitch(-20), R_Upperarm=roll(150 + 25 * math.sin(p)) @ pitch(-20),
        L_Thigh=pitch(-50 * max(0, math.sin(p))), R_Thigh=pitch(-50 * max(0, -math.sin(p))),
        L_Calf=pitch(70 * max(0, math.sin(p))), R_Calf=pitch(70 * max(0, -math.sin(p))), Spine=pitch(-10)), 12, keys=6),
    "throw_kunai": [(0, pose(R_Upperarm=pitch(-60) @ roll(30), R_Forearm=pitch(-90), Spine=yaw(25))),
                    (4, pose(R_Upperarm=pitch(-120), R_Forearm=pitch(-20), Spine=yaw(-15))),
                    (10, pose(R_Upperarm=pitch(-80), R_Forearm=pitch(-30), Spine=yaw(-5)))],
    "throw_ofuda": [(0, pose(L_Upperarm=pitch(-80), L_Forearm=pitch(-30), L_Hand=pitch(-30))),
                    (3, pose(L_Upperarm=pitch(-95), L_Forearm=pitch(-5), L_Hand=pitch(20)))],
    "throw_suzu": [(0, pose(L_Upperarm=pitch(40), L_Forearm=pitch(-30), Spine=yaw(-15))),
                   (5, pose(L_Upperarm=pitch(-110), L_Forearm=pitch(-15), Spine=yaw(15))),
                   (12, pose(L_Upperarm=pitch(-70), L_Forearm=pitch(-20)))],
    "swift_step": [(0, pose(**CROUCH)), (6, pose(drop=0.2, L_Thigh=pitch(-40), R_Thigh=pitch(-40), L_Calf=pitch(60),
                                                   R_Calf=pitch(60), L_Upperarm=roll(-60), R_Upperarm=roll(60)))],
    "ult_cast": [(0, pose(R_Upperarm=pitch(-90), R_Forearm=pitch(0), Spine=yaw(-20), L_Upperarm=roll(-20))),
                 (20, pose(R_Upperarm=pitch(-95), R_Forearm=pitch(-5), Spine=yaw(-20), L_Upperarm=roll(-20)))],
}

ORDER = ["Pelvis", "Spine", "Spine1", "Spine2", "Spine4", "Neck", "Head",
         "L_Thigh", "L_Calf", "L_Foot", "R_Thigh", "R_Calf", "R_Foot",
         "L_Clavicle", "L_Upperarm", "L_Forearm", "L_Hand", "R_Clavicle", "R_Upperarm", "R_Forearm", "R_Hand"]


def apply_pose(arm, values):
    """Sets pose bones from world-axis rotations, parents first."""
    pb = arm.pose.bones
    for b in pb:
        b.rotation_mode = "QUATERNION"
        b.rotation_quaternion = Quaternion()
        b.location = Vector()
    bpy.context.view_layer.update()
    pelvis = pb.get(B + "Pelvis")
    if pelvis is not None and values.get("drop"):
        world = arm.matrix_world @ pelvis.matrix
        world.translation.z -= values["drop"]
        pelvis.matrix = arm.matrix_world.inverted() @ world
        bpy.context.view_layer.update()
    for short in ORDER:
        q = values.get(short)
        bone = pb.get(B + short)
        if q is None or bone is None:
            continue
        world = arm.matrix_world @ bone.matrix
        rot = q.to_matrix().to_4x4()
        head = world.translation.copy()
        new = Matrix.Translation(head) @ rot @ Matrix.Translation(-head) @ world
        bone.matrix = arm.matrix_world.inverted() @ new
        bpy.context.view_layer.update()


def key_all(arm, frame):
    for b in arm.pose.bones:
        b.keyframe_insert("rotation_quaternion", frame=frame)
        b.keyframe_insert("location", frame=frame)


def add_clips(arm):
    bpy.context.scene.render.fps = FPS
    arm.animation_data_create()
    for name, keys in CLIPS.items():
        action = bpy.data.actions.new(name)
        action.use_fake_user = True
        arm.animation_data.action = action
        for frame, values in keys:
            apply_pose(arm, values)
            key_all(arm, frame)
        track = arm.animation_data.nla_tracks.new()
        track.name = name
        track.strips.new(name, int(keys[0][0]), action)
        track.mute = True
        arm.animation_data.action = None
    apply_pose(arm, {})
    print(f"OVERCRAFT_STOPGAP clips={len(CLIPS)}")
