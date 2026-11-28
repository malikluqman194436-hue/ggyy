package com.example.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectsEngine
import com.example.data.GamePreferencesRepository
import com.example.data.PlayerProgress
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.PlayGreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

  val prefsRepo = GamePreferencesRepository(application)
  val soundEngine = SoundEffectsEngine(application)

  private val _progress = MutableStateFlow(PlayerProgress())
  val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

  private val _uiState = MutableStateFlow(GameUiState())
  val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

  private var nextEntityId = 1
  private var conveyorSpeedDpPerSec = 40f
  private val perBusBoardingCooldown = mutableMapOf<Int, Float>()
  private var comboCounter = 0
  private var comboResetTimer = 0f
  private var loseGraceTimer = 0f
  private var statusBannerTimer = 0f
  private var introRingFillComplete = false
  private var armFeedLatched = false
  private var armFeedPreferRight = false
  private var laneGeometry = LaneGeometry(411f, 915f)

  init {
    LevelRepository.init(application)
    viewModelScope.launch {
      prefsRepo.progressFlow.collect { p ->
        _progress.value = p
        soundEngine.soundEnabled = p.soundEnabled
        soundEngine.vibrationEnabled = p.vibrationEnabled
        _uiState.update { state ->
          state.copy(
            coins = p.coins,
            boardBoosters = p.boardBoosters,
            refreshBoosters = p.refreshBoosters,
            vipBoosters = p.vipBoosters,
            soundEnabled = p.soundEnabled,
            vibrationEnabled = p.vibrationEnabled,
            cloudSaveEnabled = p.cloudSaveEnabled,
            languageLabel = p.languageLabel,
            passengerSizeScale = p.passengerSizeScale,
            passengerHeadScale = p.passengerHeadScale,
            passengerWidthScale = p.passengerWidthScale,
            passengerHeightScale = p.passengerHeightScale,
          )
        }
        val targetShape =
          if (_uiState.value.levelNumber >= 2) TrackLoopShape.LEVEL2_ROUNDED_OVAL
          else TrackLoopShape.LEVEL1_OVAL
        if (kotlin.math.abs(laneGeometry.passengerSizeScale - p.passengerSizeScale) > 0.001f ||
          laneGeometry.trackShape != targetShape
        ) {
          laneGeometry =
            LaneGeometry(
              widthDp = laneGeometry.widthDp,
              heightDp = laneGeometry.heightDp,
              passengerSizeScale = p.passengerSizeScale,
              trackShape = targetShape,
            )
        }
      }
    }
  }

  fun updateLaneViewport(widthDp: Float, heightDp: Float) {
    if (widthDp < 100f || heightDp < 100f) return
    val targetShape =
      if (_uiState.value.levelNumber >= 2) TrackLoopShape.LEVEL2_ROUNDED_OVAL
      else TrackLoopShape.LEVEL1_OVAL
    if (kotlin.math.abs(laneGeometry.widthDp - widthDp) < 4f &&
      kotlin.math.abs(laneGeometry.heightDp - heightDp) < 4f &&
      laneGeometry.trackShape == targetShape
    ) {
      return
    }
    val oldGeom = laneGeometry
    val newGeom = LaneGeometry(widthDp, heightDp, _uiState.value.passengerSizeScale, targetShape)
    laneGeometry = newGeom

    // Rescale active conveyor positions proportionally while keeping queued arms behind the ring curb
    val oldLeftHold = (oldGeom.leftArmLenDp - oldGeom.armHoldStopOffsetDp).coerceAtLeast(0f)
    val newLeftHold = (newGeom.leftArmLenDp - newGeom.armHoldStopOffsetDp).coerceAtLeast(0f)
    val oldRightHold = (oldGeom.rightArmLenDp - oldGeom.armHoldStopOffsetDp).coerceAtLeast(0f)
    val newRightHold = (newGeom.rightArmLenDp - newGeom.armHoldStopOffsetDp).coerceAtLeast(0f)
    val rowScale = newGeom.rowSpacingDp / oldGeom.rowSpacingDp.coerceAtLeast(1f)

    _uiState.update { state ->
      if (state.passengers.isEmpty()) return@update state
      val rescaled =
        state.passengers.map { p ->
          val newDist =
            when (p.segment) {
              ConveyorSegment.RING ->
                (p.distDp / oldGeom.ringLenDp.coerceAtLeast(1f)) * newGeom.ringLenDp
              ConveyorSegment.LEFT_ARM ->
                if (p.distDp <= oldLeftHold) {
                  newLeftHold - (oldLeftHold - p.distDp) * rowScale
                } else {
                  val frac = ((p.distDp - oldLeftHold) / (oldGeom.leftArmLenDp - oldLeftHold).coerceAtLeast(1f)).coerceIn(0f, 1f)
                  newLeftHold + frac * (newGeom.leftArmLenDp - newLeftHold)
                }
              ConveyorSegment.RIGHT_ARM ->
                if (p.distDp <= oldRightHold) {
                  newRightHold - (oldRightHold - p.distDp) * rowScale
                } else {
                  val frac = ((p.distDp - oldRightHold) / (oldGeom.rightArmLenDp - oldRightHold).coerceAtLeast(1f)).coerceIn(0f, 1f)
                  newRightHold + frac * (newGeom.rightArmLenDp - newRightHold)
                }
            }
          p.copy(distDp = newDist)
        }
      state.copy(passengers = rescaled)
    }
  }

  fun toggleLaneDebug() {
    _uiState.update { it.copy(showLaneDebug = !it.showLaneDebug) }
  }

  fun setSpeedBoosted(boosted: Boolean) {
    _uiState.update { state ->
      val effective = boosted && state.outcome == GameOutcome.PLAYING
      if (state.isSpeedBoosted == effective) state else state.copy(isSpeedBoosted = effective)
    }
  }

  fun startLevel(levelNumber: Int = _progress.value.currentLevel) {
    val levelDef = LevelRepository.getLevel(levelNumber)
    val targetShape =
      if (levelDef.levelNumber >= 2) TrackLoopShape.LEVEL2_ROUNDED_OVAL
      else TrackLoopShape.LEVEL1_OVAL
    laneGeometry =
      LaneGeometry(
        widthDp = laneGeometry.widthDp,
        heightDp = laneGeometry.heightDp,
        passengerSizeScale = _progress.value.passengerSizeScale,
        trackShape = targetShape,
      )
    nextEntityId = 1
    conveyorSpeedDpPerSec = levelDef.conveyorSpeedDpPerSec
    perBusBoardingCooldown.clear()
    comboCounter = 0
    comboResetTimer = 0f
    loseGraceTimer = 0f
    statusBannerTimer = 0f
    introRingFillComplete = true
    armFeedLatched = false
    armFeedPreferRight = false

    val vehicles =
      levelDef.vehicles.map { spec ->
        VehicleEntity(
          id = nextEntityId++,
          color = spec.color,
          direction = spec.direction,
          capacity = spec.capacity,
          x = spec.x,
          y = spec.y,
          stage = VehicleStage.IN_LOT,
        )
      }
    val tunnels =
      levelDef.tunnels.map { t ->
        TunnelState(id = nextEntityId++, side = t.side, row = t.row, queue = t.queue)
      }

    // 8 bays matching game_play_bg.png: Slot 0 = VIP, Slots 1..4 = freeSlots (4 -> 3 -> 2), Slots 5..7 = Locked
    val freeNormalCount = levelDef.freeSlots.coerceIn(2, 4)
    val slots =
      buildList {
        add(WaitingSlotState(index = 0, kind = SlotKind.VIP, unlocked = false))
        for (i in 1..4) {
          val isFree = i <= freeNormalCount
          add(
            WaitingSlotState(
              index = i,
              kind = if (isFree) SlotKind.NORMAL else SlotKind.LOCKED,
              unlocked = isFree,
            )
          )
        }
        for (i in 5..7) {
          add(WaitingSlotState(index = i, kind = SlotKind.LOCKED, unlocked = false))
        }
      }

    val passengers = buildInitialConveyorPassengers(levelDef.passengerSequence, levelDef.levelNumber)
    val initialTutorialId =
      if (levelDef.isTutorial) {
        CollisionEngine.firstUnblockedVehicleId(vehicles, levelDef.gridCols, levelDef.gridRows)
      } else {
        null
      }

    val p = _progress.value
    _uiState.update { prev ->
      GameUiState(
        levelNumber = levelDef.levelNumber,
        gridCols = levelDef.gridCols,
        gridRows = levelDef.gridRows,
        isOrthographicLot = levelDef.isOrthographicLot,
        vehicles = vehicles,
        tunnels = tunnels,
        slots = slots,
        passengers = passengers,
        totalPassengersInLevel = passengers.size,
        boardedPassengersTotal = 0,
        outcome = GameOutcome.PLAYING,
        isTutorial = levelDef.isTutorial,
        tutorialVehicleId = initialTutorialId,
        tutorialHintText = levelDef.tutorialHintText,
        coins = p.coins,
        boardBoosters = p.boardBoosters,
        refreshBoosters = p.refreshBoosters,
        vipBoosters = p.vipBoosters,
        soundEnabled = p.soundEnabled,
        vibrationEnabled = p.vibrationEnabled,
        cloudSaveEnabled = p.cloudSaveEnabled,
        languageLabel = p.languageLabel,
        passengerSizeScale = p.passengerSizeScale,
        passengerHeadScale = p.passengerHeadScale,
        passengerWidthScale = p.passengerWidthScale,
        passengerHeightScale = p.passengerHeightScale,
        showLaneDebug = prev.showLaneDebug,
        showBoardUnlockedPopup = false,
        showBoardBoosterHand = false,
      )
    }
  }

  private fun buildInitialConveyorPassengers(
    sequence: List<VehicleColor>,
    levelNumber: Int = 1,
  ): List<ConveyorPassenger> {
    val geom = laneGeometry
    val rowStep = geom.rowSpacingDp
    val rows = sequence.chunked(4)
    val maxRingRows = (geom.ringLenDp / rowStep).toInt().coerceAtLeast(12)
    val ringRowCount = minOf(rows.size, maxRingRows)
    val ringPitch = if (ringRowCount > 0) geom.ringLenDp / ringRowCount else rowStep
    val overflowCount = (rows.size - ringRowCount).coerceAtLeast(0)
    // Level 1 queues overflow on LEFT_ARM (up to 48) then RIGHT_ARM; Level 2+ fills BOTH arms evenly.
    val splitBothArmsEvenly = levelNumber >= 2
    val leftOverflowCount = overflowCount.coerceAtMost(48)
    val leftHoldDist = (geom.leftArmLenDp - geom.armHoldStopOffsetDp).coerceAtLeast(0f)
    val rightHoldDist = (geom.rightArmLenDp - geom.armHoldStopOffsetDp).coerceAtLeast(0f)

    val result = ArrayList<ConveyorPassenger>(sequence.size)
    var qIdx = 0
    var leftRank = 0
    var rightRank = 0

    for (rowIdx in rows.indices) {
      val rowColors = rows[rowIdx]
      val (segment, distDp, branch) =
        when {
          rowIdx < ringRowCount -> {
            // Evenly fill 100% of the center loop with zero seam gap at t = 0
            val d = geom.wrapRing(geom.boardingGateCenterDp - rowIdx * ringPitch)
            Triple(ConveyorSegment.RING, d, 0)
          }
          splitBothArmsEvenly -> {
            val overflowIdx = rowIdx - ringRowCount
            if (overflowIdx % 2 == 0) {
              val d = leftHoldDist - (leftRank++) * rowStep
              Triple(ConveyorSegment.LEFT_ARM, d, 0)
            } else {
              val d = rightHoldDist - (rightRank++) * rowStep
              Triple(ConveyorSegment.RIGHT_ARM, d, 1)
            }
          }
          rowIdx < ringRowCount + leftOverflowCount -> {
            val rank = rowIdx - ringRowCount
            val d = leftHoldDist - rank * rowStep
            Triple(ConveyorSegment.LEFT_ARM, d, 0)
          }
          else -> {
            val rank = rowIdx - (ringRowCount + leftOverflowCount)
            val d = rightHoldDist - rank * rowStep
            Triple(ConveyorSegment.RIGHT_ARM, d, 1)
          }
        }

      for (col in rowColors.indices) {
        result.add(
          ConveyorPassenger(
            id = nextEntityId++,
            color = rowColors[col],
            queueIndex = qIdx++,
            trackPos = 0f,
            rowGroupId = rowIdx,
            branch = branch,
            colInRow = col,
            segment = segment,
            distDp = distDp,
            isWalking = segment == ConveyorSegment.RING,
          )
        )
      }
    }
    return result
  }

  fun onFrame(dtSecRaw: Float) {
    val state = _uiState.value
    if (state.outcome != GameOutcome.PLAYING) return

    val dt = dtSecRaw.coerceIn(0.001f, 0.05f)

    // Update timers
    val cdKeys = perBusBoardingCooldown.keys.toList()
    for (k in cdKeys) {
      val rem = (perBusBoardingCooldown[k] ?: 0f) - dt
      if (rem <= 0f) perBusBoardingCooldown.remove(k) else perBusBoardingCooldown[k] = rem
    }
    if (comboResetTimer > 0f) {
      comboResetTimer -= dt
      if (comboResetTimer <= 0f) comboCounter = 0
    }
    if (statusBannerTimer > 0f) {
      statusBannerTimer -= dt
    }

    var vehicles = state.vehicles.toMutableList()
    var tunnels = state.tunnels.toMutableList()
    var slots = state.slots.toMutableList()
    var passengers = state.passengers.toMutableList()
    var jumps = state.jumpingPassengers.toMutableList()
    var particles = state.particles.toMutableList()
    var floats = state.floatingTexts.toMutableList()
    var boardedTotal = state.boardedPassengersTotal
    var tutorialId = state.tutorialVehicleId

    // 1. Update vehicles (bump/shake/flash, spawning, exiting, driving to slot, bounce, departing)
    val vehiclesToRemove = mutableSetOf<Int>()
    for (i in vehicles.indices) {
      var v = vehicles[i]
      if (v.flashRedTimer > 0f) {
        v = v.copy(flashRedTimer = (v.flashRedTimer - dt).coerceAtLeast(0f))
      }
      if (v.shakePhase > 0f) {
        val nextPhase = (v.shakePhase - dt * 3.5f).coerceAtLeast(0f)
        val progress = 1f - nextPhase
        val lunge =
          if (progress < 0.26f) (progress / 0.26f)
          else ((1f - progress) / 0.74f).coerceAtLeast(0f)
        val targetDist = if (v.bumpMaxDist > 0f) v.bumpMaxDist else v.bumpOffset
        v =
          v.copy(
            shakePhase = nextPhase,
            bumpOffset = if (nextPhase <= 0f) 0f else targetDist * lunge,
            bumpMaxDist = if (nextPhase <= 0f) 0f else v.bumpMaxDist,
            squashFactor = if (nextPhase <= 0f) 1f else 1f - 0.10f * sin(lunge * PI * 0.5).toFloat(),
          )
      } else if (v.bounceTimer > 0f) {
        val bt = (v.bounceTimer - dt * 3.5f).coerceAtLeast(0f)
        v = v.copy(bounceTimer = bt, squashFactor = 1f + 0.06f * sin(bt * PI * 2f).toFloat())
      } else if (v.squashFactor != 1f) {
        v = v.copy(squashFactor = 1f)
      }

      when (v.stage) {
        VehicleStage.INTRO_SLIDING -> {
          if (v.introDelayRemaining > 0f) {
            v = v.copy(introDelayRemaining = (v.introDelayRemaining - dt).coerceAtLeast(0f))
          } else {
            val p = (v.animProgress + dt * 3.0f).coerceAtMost(1f)
            val eased = 1f - (1f - p) * (1f - p) * (1f - p)
            val ny = v.startY + (v.targetY - v.startY) * eased
            v =
              if (p >= 1f) {
                v.copy(
                  x = v.targetX,
                  y = v.targetY,
                  animProgress = 1f,
                  stage = VehicleStage.IN_LOT,
                  bounceTimer = 0.14f,
                )
              } else {
                v.copy(y = ny, animProgress = p)
              }
          }
        }

        VehicleStage.SPAWNING_FROM_TUNNEL -> {
          val p = (v.animProgress + dt * 2.8f).coerceAtMost(1f)
          val eased = 1f - (1f - p) * (1f - p)
          val nx = v.startX + (v.targetX - v.startX) * eased
          val ny = v.startY + (v.targetY - v.startY) * eased
          v =
            if (p >= 1f) {
              v.copy(x = v.targetX, y = v.targetY, animProgress = 1f, stage = VehicleStage.IN_LOT)
            } else {
              v.copy(x = nx, y = ny, animProgress = p)
            }
        }

        VehicleStage.DRIVING_TO_SLOT -> {
          val p = (v.animProgress + dt * 1.32f).coerceAtMost(1f)
          v =
            if (p >= 1f) {
              v.copy(
                animProgress = 1f,
                stage = VehicleStage.PARKED,
                bounceTimer = 0.24f,
                roofOpenProgress = 0f,
              )
            } else {
              v.copy(animProgress = p, roofOpenProgress = 0f)
            }
        }

        VehicleStage.PARKED -> {
          if (v.roofOpenProgress < 1f) {
            v = v.copy(roofOpenProgress = (v.roofOpenProgress + dt / 0.18f).coerceAtMost(1f))
          }
        }

        VehicleStage.DEPARTING_FULL -> {
          // Crisp roof close in slot (p <= DEPART_CLOSE_FRACTION), then fast pull-out & departure freeing the slot
          val p = (v.animProgress + dt / 0.78f).coerceAtMost(1f)
          val closingRoof = (1f - p / DEPART_CLOSE_FRACTION).coerceIn(0f, 1f)
          if (p >= DEPART_CLOSE_FRACTION) {
            val sIdx = v.slotIndex
            if (sIdx in slots.indices && slots[sIdx].occupantVehicleId == v.id) {
              slots[sIdx] = slots[sIdx].copy(occupantVehicleId = null)
            }
          }
          v = v.copy(animProgress = p, roofOpenProgress = closingRoof)
          if (p >= 1f) {
            vehiclesToRemove.add(v.id)
          }
        }

        else -> {}
      }
      vehicles[i] = v
    }
    if (vehiclesToRemove.isNotEmpty()) {
      vehicles.removeAll { it.id in vehiclesToRemove }
    }

    // 2. Spawn queued vehicles from tunnels when exit cell is clear
    val lotBlockers =
      vehicles.filter {
        it.stage == VehicleStage.IN_LOT || it.stage == VehicleStage.SPAWNING_FROM_TUNNEL
      }
    for (tIdx in tunnels.indices) {
      val tunnel = tunnels[tIdx]
      if (tunnel.queue.isEmpty()) continue
      val nextSpec = tunnel.queue.first()
      val length = vehicleLengthForCapacity(nextSpec.capacity)
      val spawnTargetX =
        if (tunnel.side == TunnelSide.LEFT) length * 0.5f + 0.12f
        else state.gridCols - length * 0.5f - 0.12f
      val spawnY = tunnel.row + 0.5f
      val dummy =
        VehicleEntity(
          id = -1,
          color = nextSpec.color,
          direction = nextSpec.direction,
          capacity = nextSpec.capacity,
          x = spawnTargetX,
          y = spawnY,
        )
      val box = CollisionEngine.boxForVehicle(dummy)
      val blocked = lotBlockers.any { CollisionEngine.overlaps(box, CollisionEngine.boxForVehicle(it)) }
      if (!blocked) {
        val startX = if (tunnel.side == TunnelSide.LEFT) -1.0f else state.gridCols + 1.0f
        val spawnedVehicle =
          dummy.copy(
            id = nextEntityId++,
            x = startX,
            y = spawnY,
            startX = startX,
            startY = spawnY,
            targetX = spawnTargetX,
            targetY = spawnY,
            stage = VehicleStage.SPAWNING_FROM_TUNNEL,
            animProgress = 0f,
          )
        vehicles.add(spawnedVehicle)
        tunnels[tIdx] = tunnel.copy(queue = tunnel.queue.drop(1))
      }
    }

    // 3. Advance conveyor passengers cleanly BY ROW (all 4 columns in a row stay strictly abreast)
    val geom = laneGeometry
    val rowStep = geom.rowSpacingDp
    val maxRingRows = (geom.ringLenDp / rowStep).toInt().coerceAtLeast(12)
    val effectiveConveyorSpeed =
      if (state.isSpeedBoosted) conveyorSpeedDpPerSec * 3.0f else conveyorSpeedDpPerSec
    val moveStep = effectiveConveyorSpeed * dt

    // 3a. Advance circular RING rows as synchronized 4-column rows
    val ringByRow = LinkedHashMap<Int, MutableList<Int>>()
    for (idx in passengers.indices) {
      if (passengers[idx].segment == ConveyorSegment.RING) {
        ringByRow.getOrPut(passengers[idx].rowGroupId) { mutableListOf() }.add(idx)
      }
    }
    if (ringByRow.isNotEmpty()) {
      val sortedRows =
        ringByRow.entries
          .map { (rowId, idxs) ->
            rowId to geom.wrapRing(passengers[idxs.first()].distDp) to idxs
          }
          .sortedBy { it.first.second }

      var maxGap = -1f
      var headPosInSorted = 0
      if (sortedRows.size >= 2) {
        for (k in sortedRows.indices) {
          val curDist = sortedRows[k].first.second
          val nextDist = sortedRows[(k + 1) % sortedRows.size].first.second
          val gap = geom.forwardRingDistance(curDist, nextDist)
          if (gap > maxGap) {
            maxGap = gap
            headPosInSorted = k
          }
        }
      }

      var prevRowMovedDist = 0f
      for (step in sortedRows.indices) {
        val posInSorted = (headPosInSorted - step + sortedRows.size) % sortedRows.size
        val (_, curRowDist) = sortedRows[posInSorted].first
        val rowIndices = sortedRows[posInSorted].second
        var nextDist = geom.wrapRing(curRowDist + moveStep)
        if (step > 0) {
          val gapAhead = geom.forwardRingDistance(nextDist, prevRowMovedDist)
          if (gapAhead > rowStep * 1.02f && gapAhead < geom.ringLenDp * 0.45f) {
            val extra = ((gapAhead - rowStep) * 9.0f * dt).coerceAtMost(moveStep * 2.6f)
            nextDist = geom.wrapRing(nextDist + extra)
          }
          val postGap = geom.forwardRingDistance(nextDist, prevRowMovedDist)
          if (postGap < rowStep * 0.98f) {
            nextDist = geom.wrapRing(prevRowMovedDist - rowStep)
          }
        }
        for (idx in rowIndices) {
          passengers[idx] = passengers[idx].copy(distDp = nextDist, isWalking = true)
        }
        prevRowMovedDist = nextDist
      }
    }

    // 3b. Hold feeder arms cleanly outside ring curbs until room opens on the ring; drain LEFT_ARM before RIGHT_ARM
    var currentRingRowCount = ringByRow.size
    val leftArmHasRows = passengers.any { it.segment == ConveyorSegment.LEFT_ARM }
    armFeedLatched = currentRingRowCount < maxRingRows
    val ringOpenForArmFeed = armFeedLatched

    fun advanceArm(seg: ConveyorSegment, armLen: Float, entryRingDist: Float, allowRingEntry: Boolean, allowAdvance: Boolean): Boolean {
      val armByRow = LinkedHashMap<Int, MutableList<Int>>()
      for (idx in passengers.indices) {
        if (passengers[idx].segment == seg) {
          armByRow.getOrPut(passengers[idx].rowGroupId) { mutableListOf() }.add(idx)
        }
      }
      if (armByRow.isEmpty()) return false

      val sortedArmRows =
        armByRow.entries
          .map { (rowId, idxs) ->
            Triple(rowId, passengers[idxs.first()].distDp, idxs)
          }
          .sortedByDescending { it.second }

      val holdStopDist = (armLen - geom.armHoldStopOffsetDp).coerceAtLeast(0f)
      val transitDist = (armLen - holdStopDist).coerceAtLeast(1f)
      if (!allowAdvance) {
        // Freeze this arm completely in idle standing pose until permitted to move (Level 1 RIGHT_ARM waits for LEFT_ARM)
        for ((_, _, rowIndices) in sortedArmRows) {
          for (idx in rowIndices) {
            if (passengers[idx].isWalking) {
              passengers[idx] = passengers[idx].copy(isWalking = false)
            }
          }
        }
        return false
      }

      var enteredRing = false
      val entrySpeedMult = 1.65f
      var blockerDistOnArm = Float.MAX_VALUE
      for (rank in sortedArmRows.indices) {
        val (_, curDist, rowIndices) = sortedArmRows[rank]
        val alreadyCrossing = rank == 0 && curDist > holdStopDist + 0.05f
        val stepMultiplier = if (alreadyCrossing) entrySpeedMult else 1.0f
        var candidate = curDist + moveStep * stepMultiplier
        if (!alreadyCrossing && blockerDistOnArm < Float.MAX_VALUE) {
          val maxTarget = min(blockerDistOnArm - rowStep, holdStopDist)
          val gapAheadArm = (maxTarget - candidate).coerceAtLeast(0f)
          if (gapAheadArm > 0.02f) {
            candidate += (gapAheadArm * 9.0f * dt).coerceAtMost(moveStep * 2.5f)
          }
        }

        if (rank == 0 && candidate >= holdStopDist) {
          if (alreadyCrossing) {
            if (candidate >= armLen) {
              val ringStart = geom.wrapRing(entryRingDist + (candidate - armLen))
              for (idx in rowIndices) {
                passengers[idx] = passengers[idx].copy(segment = ConveyorSegment.RING, distDp = ringStart, isWalking = true)
              }
              currentRingRowCount++
              enteredRing = true
              blockerDistOnArm = armLen + (candidate - armLen)
              continue
            }
          } else {
            val canEnterByCapacity = allowRingEntry && ringOpenForArmFeed && currentRingRowCount < maxRingRows
            val ringDriftDuringEntry = transitDist / entrySpeedMult
            val ringClear =
              canEnterByCapacity &&
                passengers.none { other ->
                  if (other.segment != ConveyorSegment.RING) return@none false
                  val arrivalDist = geom.wrapRing(other.distDp + ringDriftDuringEntry)
                  val arrBehind = geom.forwardRingDistance(arrivalDist, entryRingDist)
                  val arrAhead = geom.forwardRingDistance(entryRingDist, arrivalDist)
                  arrBehind < rowStep * 0.90f || arrAhead < rowStep * 0.90f
                }
            if (ringClear) {
              candidate = holdStopDist + moveStep * entrySpeedMult
              if (candidate >= armLen) {
                val ringStart = geom.wrapRing(entryRingDist + (candidate - armLen))
                for (idx in rowIndices) {
                  passengers[idx] = passengers[idx].copy(segment = ConveyorSegment.RING, distDp = ringStart, isWalking = true)
                }
                currentRingRowCount++
                enteredRing = true
                blockerDistOnArm = armLen + (candidate - armLen)
                continue
              }
            } else {
              candidate = holdStopDist
            }
          }
        }

        // Only rank == 0 when cleared may cross holdStopDist; all waiting rows behind stay strictly <= holdStopDist
        val maxAllowed =
          if (rank == 0) blockerDistOnArm - rowStep
          else min(blockerDistOnArm - rowStep, holdStopDist)
        val clamped = min(candidate, maxAllowed)
        val moved = clamped > curDist + 0.001f
        for (idx in rowIndices) {
          passengers[idx] = passengers[idx].copy(distDp = clamped, isWalking = moved)
        }
        blockerDistOnArm = clamped
      }
      return enteredRing
    }

    if (state.levelNumber >= 2) {
      if (armFeedPreferRight) {
        val rightEntered = advanceArm(ConveyorSegment.RIGHT_ARM, geom.rightArmLenDp, geom.rightEntryRingDistDp, true, true)
        val leftEntered = advanceArm(ConveyorSegment.LEFT_ARM, geom.leftArmLenDp, geom.leftEntryRingDistDp, true, true)
        if (rightEntered && !leftEntered) armFeedPreferRight = false
      } else {
        val leftEntered = advanceArm(ConveyorSegment.LEFT_ARM, geom.leftArmLenDp, geom.leftEntryRingDistDp, true, true)
        val rightEntered = advanceArm(ConveyorSegment.RIGHT_ARM, geom.rightArmLenDp, geom.rightEntryRingDistDp, true, true)
        if (leftEntered && !rightEntered) armFeedPreferRight = true
      }
    } else {
      advanceArm(
        seg = ConveyorSegment.LEFT_ARM,
        armLen = geom.leftArmLenDp,
        entryRingDist = geom.leftEntryRingDistDp,
        allowRingEntry = true,
        allowAdvance = true,
      )
      advanceArm(
        seg = ConveyorSegment.RIGHT_ARM,
        armLen = geom.rightArmLenDp,
        entryRingDist = geom.rightEntryRingDistDp,
        allowRingEntry = !leftArmHasRows,
        allowAdvance = !leftArmHasRows,
      )
    }

    // 4. Orderly 4-by-4 whole-row boarding inside the front boarding gate (never splits a matching 4-row)
    val gateRows =
      passengers
        .withIndex()
        .mapNotNull { indexed ->
          val offset = geom.signedGateOffsetDp(indexed.value.segment, indexed.value.distDp) ?: return@mapNotNull null
          Triple(indexed.index, indexed.value, offset)
        }
        .groupBy { it.second.rowGroupId }
        .values
        .map { group ->
          val sortedMembers = group.sortedBy { it.second.colInRow }
          val gateOffset = sortedMembers.first().third
          sortedMembers to gateOffset
        }
        .sortedByDescending { it.second }

    val removedIndices = mutableListOf<Int>()
    val busesUsedThisFrame = mutableSetOf<Int>()
    for ((rowMembers, _) in gateRows) {
      val rowColor = rowMembers.first().second.color
      val uniformRow = rowMembers.all { it.second.color == rowColor }
      if (uniformRow) {
        val matchingBusIndices =
          vehicles
            .withIndex()
            .filter { (_, v) ->
              v.stage == VehicleStage.PARKED &&
                v.roofOpenProgress >= 0.14f &&
                v.color == rowColor &&
                v.reservedCount < v.capacity &&
                (perBusBoardingCooldown[v.id] ?: 0f) <= 0f
            }
            .sortedBy { (_, v) -> v.slotIndex }
            .map { it.index }

        val totalSeatsAvail = matchingBusIndices.sumOf { vehicles[it].capacity - vehicles[it].reservedCount }
        if (totalSeatsAvail <= 0) continue

        // Board the whole 4-wide row together when seats allow (never leave 1-2 stragglers on a full row)
        val boardCount = minOf(rowMembers.size, totalSeatsAvail)
        if (boardCount < rowMembers.size && totalSeatsAvail < rowMembers.size) {
          // Partial last-seat fill only if the final matching bus only needed < 4 seats
          for (mIdx in 0 until boardCount) {
            val (passIdx, candidate, _) = rowMembers[mIdx]
            val targetBusIdx =
              matchingBusIndices.firstOrNull { vehicles[it].reservedCount < vehicles[it].capacity } ?: break
            val bus = vehicles[targetBusIdx]
            vehicles[targetBusIdx] = bus.copy(reservedCount = bus.reservedCount + 1)
            busesUsedThisFrame.add(bus.id)
            removedIndices.add(passIdx)

            val (_, posDp) = geom.passengerCoords(candidate.segment, candidate.distDp, candidate.colInRow)
            jumps.add(
              JumpingPassenger(
                id = nextEntityId++,
                color = candidate.color,
                targetVehicleId = bus.id,
                targetSlotIndex = bus.slotIndex,
                startNormX = (posDp.first / geom.widthDp).coerceIn(0.05f, 0.95f),
                startNormY = (posDp.second / geom.heightDp).coerceIn(0.05f, 0.44f),
                progress = 0f,
                speed = 1f / (0.16f + mIdx * 0.022f),
              )
            )
            loseGraceTimer = 0f
          }
        } else {
          for ((mIdx, triple) in rowMembers.withIndex()) {
            val (passIdx, candidate, _) = triple
            val targetBusIdx =
              matchingBusIndices.firstOrNull { vehicles[it].reservedCount < vehicles[it].capacity } ?: break
            val bus = vehicles[targetBusIdx]
            vehicles[targetBusIdx] = bus.copy(reservedCount = bus.reservedCount + 1)
            busesUsedThisFrame.add(bus.id)
            removedIndices.add(passIdx)

            val (_, posDp) = geom.passengerCoords(candidate.segment, candidate.distDp, candidate.colInRow)
            jumps.add(
              JumpingPassenger(
                id = nextEntityId++,
                color = candidate.color,
                targetVehicleId = bus.id,
                targetSlotIndex = bus.slotIndex,
                startNormX = (posDp.first / geom.widthDp).coerceIn(0.05f, 0.95f),
                startNormY = (posDp.second / geom.heightDp).coerceIn(0.05f, 0.44f),
                progress = 0f,
                speed = 1f / (0.16f + mIdx * 0.022f),
              )
            )
            loseGraceTimer = 0f
          }
        }
      } else {
        for ((mIdx, triple) in rowMembers.withIndex()) {
          val (passIdx, candidate, _) = triple
          val matchingBusIdx =
            vehicles
              .withIndex()
              .filter { (_, v) ->
                v.stage == VehicleStage.PARKED &&
                  v.roofOpenProgress >= 0.14f &&
                  v.color == candidate.color &&
                  v.reservedCount < v.capacity &&
                  (perBusBoardingCooldown[v.id] ?: 0f) <= 0f
              }
              .minByOrNull { (_, v) -> v.slotIndex }
              ?.index
          if (matchingBusIdx != null) {
            val bus = vehicles[matchingBusIdx]
            vehicles[matchingBusIdx] = bus.copy(reservedCount = bus.reservedCount + 1)
            busesUsedThisFrame.add(bus.id)
            removedIndices.add(passIdx)

            val (_, posDp) = geom.passengerCoords(candidate.segment, candidate.distDp, candidate.colInRow)
            jumps.add(
              JumpingPassenger(
                id = nextEntityId++,
                color = candidate.color,
                targetVehicleId = bus.id,
                targetSlotIndex = bus.slotIndex,
                startNormX = (posDp.first / geom.widthDp).coerceIn(0.05f, 0.95f),
                startNormY = (posDp.second / geom.heightDp).coerceIn(0.05f, 0.44f),
                progress = 0f,
                speed = 1f / (0.16f + mIdx * 0.022f),
              )
            )
            loseGraceTimer = 0f
          }
        }
      }
    }
    for (busId in busesUsedThisFrame) {
      perBusBoardingCooldown[busId] = 0.08f
    }
    if (removedIndices.isNotEmpty()) {
      val removeSet = removedIndices.toHashSet()
      passengers = passengers.filterIndexed { index, _ -> index !in removeSet }.toMutableList()
      for (k in passengers.indices) {
        passengers[k] = passengers[k].copy(queueIndex = k)
      }
    }

    // 5. Update jumping passengers (0.30s arc to bus roof)
    val finishedJumps = mutableListOf<JumpingPassenger>()
    for (i in jumps.indices) {
      val j = jumps[i]
      val nextP = j.progress + dt * j.speed
      if (nextP >= 1f) {
        finishedJumps.add(j)
      } else {
        jumps[i] = j.copy(progress = nextP)
      }
    }
    if (finishedJumps.isNotEmpty()) {
      jumps.removeAll(finishedJumps)
      for (j in finishedJumps) {
        val vIdx = vehicles.indexOfFirst { it.id == j.targetVehicleId }
        if (vIdx >= 0) {
          val bus = vehicles[vIdx]
          if (bus.stage == VehicleStage.DEPARTING_FULL) continue
          val newBoarded = (bus.boardedCount + 1).coerceAtMost(bus.capacity)
          boardedTotal++
          soundEngine.playPop(comboCounter)
          comboCounter = (comboCounter + 1).coerceAtMost(9)
          comboResetTimer = 0.8f

          val slotXNorm = slotNormalizedX(j.targetSlotIndex, slots.size)
          // Tiny roof pop sparkle on every passenger entry
          repeat(3) {
            particles.add(
              Particle(
                id = nextEntityId++,
                x = slotXNorm + (Random.nextFloat() - 0.5f) * 0.025f,
                y = 0.332f,
                vx = (Random.nextFloat() - 0.5f) * 0.14f,
                vy = -0.08f - Random.nextFloat() * 0.10f,
                color = Color.White,
                sizeDp = 5f,
                life = 0.24f,
                maxLife = 0.24f,
              )
            )
          }

          if (newBoarded >= bus.capacity) {
            soundEngine.playDing()
            vehicles[vIdx] =
              bus.copy(
                boardedCount = newBoarded,
                stage = VehicleStage.DEPARTING_FULL,
                animProgress = 0f,
              )
            floats.add(
              FloatingText(
                id = nextEntityId++,
                x = slotXNorm,
                y = 0.315f,
                text = "FULL!",
                color = GoldCoin,
                life = 0.9f,
                maxLife = 0.9f,
              )
            )
            spawnSmokeCloud(particles, slotXNorm, 0.359f, count = 10)
            spawnConfettiBurst(particles, slotXNorm, 0.352f, count = 18)
          } else {
            vehicles[vIdx] = bus.copy(boardedCount = newBoarded, bounceTimer = 0.18f)
          }
        }
      }
    }

    // 6. Update particles and floating text
    val aliveParticles = mutableListOf<Particle>()
    for (p in particles) {
      val nl = p.life - dt
      if (nl > 0f) {
        if (p.isSmokePuff) {
          val drag = (1f - 2.6f * dt).coerceAtLeast(0.75f)
          aliveParticles.add(
            p.copy(
              x = p.x + p.vx * dt,
              y = p.y + p.vy * dt,
              vx = p.vx * drag,
              vy = p.vy * drag - 0.12f * dt,
              life = nl,
            )
          )
        } else {
          aliveParticles.add(
            p.copy(
              x = p.x + p.vx * dt,
              y = p.y + p.vy * dt,
              vy = p.vy + 0.55f * dt,
              life = nl,
            )
          )
        }
      }
    }
    particles = aliveParticles

    val aliveFloats = mutableListOf<FloatingText>()
    for (f in floats) {
      val nl = f.life - dt
      if (nl > 0f) {
        aliveFloats.add(f.copy(y = f.y - 0.12f * dt, life = nl))
      }
    }
    floats = aliveFloats

    // Update tutorial hint vehicle if cleared
    if (state.isTutorial) {
      val stillInLot =
        vehicles.any {
          it.id == tutorialId && (it.stage == VehicleStage.IN_LOT || it.stage == VehicleStage.INTRO_SLIDING)
        }
      if (!stillInLot) {
        tutorialId = CollisionEngine.firstUnblockedVehicleId(vehicles, state.gridCols, state.gridRows)
      }
    }

    // 7. Check Win & Lose conditions
    var nextOutcome = GameOutcome.PLAYING
    var starsEarned = state.starsEarned

    val allDone =
      passengers.isEmpty() &&
        jumps.isEmpty() &&
        vehicles.none {
          it.stage == VehicleStage.INTRO_SLIDING ||
            it.stage == VehicleStage.IN_LOT ||
            it.stage == VehicleStage.SPAWNING_FROM_TUNNEL ||
            it.stage == VehicleStage.EXITING_LOT ||
            it.stage == VehicleStage.DRIVING_TO_SLOT ||
            it.stage == VehicleStage.PARKED ||
            it.stage == VehicleStage.DEPARTING_FULL
        }

    if (allDone) {
      nextOutcome = GameOutcome.WON
      starsEarned =
        when {
          !state.usedRevive && state.bumpsCount <= 2 -> 3
          !state.usedRevive -> 2
          else -> 1
        }
      soundEngine.playWin()
      spawnConfettiBurst(particles, 0.3f, 0.35f, 24)
      spawnConfettiBurst(particles, 0.7f, 0.35f, 24)
      viewModelScope.launch {
        prefsRepo.adjustCoins(50)
        if (state.levelNumber >= _progress.value.currentLevel) {
          prefsRepo.saveCurrentLevel(state.levelNumber + 1)
        }
      }
    } else {
      val anyTransit =
        jumps.isNotEmpty() ||
          vehicles.any {
            it.stage == VehicleStage.EXITING_LOT ||
              it.stage == VehicleStage.DRIVING_TO_SLOT ||
              it.stage == VehicleStage.DEPARTING_FULL
          }
      val anyFreeSlot = slots.any { it.unlocked && it.occupantVehicleId == null }

      if (!anyTransit && !anyFreeSlot && passengers.isNotEmpty()) {
        val parkedColors =
          vehicles
            .filter { it.stage == VehicleStage.PARKED && it.reservedCount < it.capacity }
            .map { it.color }
            .toSet()
        // Any passenger on the ring can circulate past the boarding zone; arm passengers can only enter if ring advances
        val ringHasMatching =
          passengers.any { it.segment == ConveyorSegment.RING && it.color in parkedColors }
        if (!ringHasMatching) {
          // If ring has open capacity and the next reachable feeder rows match parked buses, keep playing
          val ringRowCountNow =
            passengers.asSequence().filter { it.segment == ConveyorSegment.RING }.map { it.rowGroupId }.distinct().count()
          val availableRingSlots = (maxRingRows - ringRowCountNow).coerceAtLeast(0)
          val reachableArmRows =
            (passengers.asSequence()
              .filter { it.segment == ConveyorSegment.LEFT_ARM }
              .sortedByDescending { it.distDp }
              .map { it.rowGroupId }
              .distinct() +
              passengers.asSequence()
                .filter { it.segment == ConveyorSegment.RIGHT_ARM }
                .sortedByDescending { it.distDp }
                .map { it.rowGroupId }
                .distinct())
              .take(availableRingSlots)
              .toHashSet()
          val canArmReach =
            reachableArmRows.isNotEmpty() &&
              passengers.any { it.rowGroupId in reachableArmRows && it.color in parkedColors }
          if (!canArmReach) {
            loseGraceTimer += dt
            if (loseGraceTimer >= 0.65f) {
              nextOutcome = GameOutcome.LOST
              soundEngine.playLose()
            }
          } else {
            loseGraceTimer = 0f
          }
        } else {
          loseGraceTimer = 0f
        }
      } else {
        loseGraceTimer = 0f
      }
    }

    _uiState.update {
      it.copy(
        vehicles = vehicles,
        tunnels = tunnels,
        slots = slots,
        passengers = passengers,
        jumpingPassengers = jumps,
        particles = particles,
        floatingTexts = floats,
        boardedPassengersTotal = boardedTotal,
        conveyorOffsetPx = (it.conveyorOffsetPx + effectiveConveyorSpeed * dt) % 12000f,
        outcome = nextOutcome,
        isSpeedBoosted = if (nextOutcome == GameOutcome.PLAYING) it.isSpeedBoosted else false,
        starsEarned = starsEarned,
        tutorialVehicleId = tutorialId,
        statusBannerText = if (statusBannerTimer > 0f) it.statusBannerText else null,
      )
    }
  }

  fun onLotTap(gridX: Float, gridY: Float) {
    val state = _uiState.value
    if (state.outcome != GameOutcome.PLAYING) return

    val tapped =
      state.vehicles.firstOrNull { v ->
        v.stage == VehicleStage.IN_LOT && CollisionEngine.pointHitsVehicle(gridX, gridY, v)
      } ?: return

    val lotBlockers =
      state.vehicles.mapNotNull {
        when (it.stage) {
          VehicleStage.IN_LOT, VehicleStage.SPAWNING_FROM_TUNNEL -> it
          VehicleStage.INTRO_SLIDING -> it.copy(x = it.targetX, y = it.targetY)
          else -> null
        }
      }
    val sweep = CollisionEngine.checkPath(tapped, lotBlockers, state.gridCols, state.gridRows)

    if (!sweep.isClear) {
      soundEngine.playBonk()
      val lungeDist = (sweep.freeDistance + 0.08f).coerceIn(0.14f, 0.34f)
      val impactAdvance = sweep.freeDistance.coerceAtMost(0.55f) + tapped.lengthInCells * 0.44f
      val impactGx = tapped.x + tapped.direction.dx * impactAdvance
      val impactGy = tapped.y + tapped.direction.dy * impactAdvance
      val (impactNormX, impactNormY) =
        isoGridToPlayfieldNorm(
          impactGx,
          impactGy,
          state.gridCols,
          state.gridRows,
          state.isOrthographicLot,
        )

      val collisionParticles = state.particles.toMutableList()
      spawnCollisionEffects(collisionParticles, impactNormX, impactNormY)

      val updated =
        state.vehicles.map { v ->
          when (v.id) {
            tapped.id ->
              v.copy(
                bumpOffset = 0f,
                bumpMaxDist = lungeDist,
                shakePhase = 1f,
              )
            sweep.blockerId ->
              v.copy(
                flashRedTimer = 0.36f,
                bumpOffset = 0f,
                bumpMaxDist = 0.06f,
                shakePhase = 0.65f,
              )
            else -> v
          }
        }
      _uiState.update {
        it.copy(
          vehicles = updated,
          particles = collisionParticles,
          bumpsCount = it.bumpsCount + 1,
        )
      }
      return
    }

    val freeSlotIndex = state.slots.indexOfFirst { it.unlocked && it.occupantVehicleId == null }
    if (freeSlotIndex < 0) {
      soundEngine.playBonk()
      statusBannerTimer = 1.6f
      val (nx, ny) =
        isoGridToPlayfieldNorm(
          tapped.x,
          tapped.y,
          state.gridCols,
          state.gridRows,
          state.isOrthographicLot,
        )
      val fullBayParticles = state.particles.toMutableList()
      spawnCollisionEffects(fullBayParticles, nx, ny)
      val updated =
        state.vehicles.map { v ->
          if (v.id == tapped.id) v.copy(bumpOffset = 0f, bumpMaxDist = 0.14f, shakePhase = 0.9f) else v
        }
      _uiState.update {
        it.copy(
          vehicles = updated,
          particles = fullBayParticles,
          statusBannerText = "Waiting bay full! Board passengers or unlock a slot.",
        )
      }
      return
    }

    soundEngine.playBusTap()
    val updatedSlots = state.slots.toMutableList()
    updatedSlots[freeSlotIndex] = updatedSlots[freeSlotIndex].copy(occupantVehicleId = tapped.id)

    val updatedVehicles =
      state.vehicles.map { v ->
        if (v.id == tapped.id) {
          v.copy(
            stage = VehicleStage.DRIVING_TO_SLOT,
            slotIndex = freeSlotIndex,
            startX = v.x,
            startY = v.y,
            targetX = v.x,
            targetY = v.y,
            animProgress = 0f,
          )
        } else {
          v
        }
      }

    // Spawn dust puff particles at tapped vehicle
    val newParticles = state.particles.toMutableList()
    val (normX, normY) =
      isoGridToPlayfieldNorm(
        tapped.x,
        tapped.y,
        state.gridCols,
        state.gridRows,
        state.isOrthographicLot,
      )
    repeat(6) {
      newParticles.add(
        Particle(
          id = nextEntityId++,
          x = normX,
          y = normY,
          vx = (Random.nextFloat() - 0.5f) * 0.25f,
          vy = (Random.nextFloat() - 0.5f) * 0.25f,
          color = Color(0xFFE5ECF5),
          sizeDp = 7f,
          life = 0.4f,
          maxLife = 0.4f,
        )
      )
    }

    _uiState.update {
      it.copy(
        vehicles = updatedVehicles,
        slots = updatedSlots,
        particles = newParticles,
      )
    }
  }

  fun onSlotTapped(slotIndex: Int) {
    val state = _uiState.value
    if (state.outcome != GameOutcome.PLAYING) return
    val slot = state.slots.getOrNull(slotIndex) ?: return
    if (slot.unlocked) return

    soundEngine.playClick()
    if (slot.kind == SlotKind.VIP) {
      onBoosterTapped(BoosterType.VIP_SLOT)
    } else if (slot.kind == SlotKind.LOCKED) {
      _uiState.update { it.copy(activeSlotUnlockIndex = slotIndex, shopNotice = null) }
    }
  }

  fun confirmUnlockSlot(slotIndex: Int) {
    val state = _uiState.value
    soundEngine.playClick()
    soundEngine.playUnlock()
    val updatedSlots =
      state.slots.mapIndexed { idx, s ->
        if (idx == slotIndex) s.copy(unlocked = true) else s
      }
    statusBannerTimer = 1.6f
    _uiState.update {
      it.copy(
        slots = updatedSlots,
        activeSlotUnlockIndex = null,
        shopNotice = null,
        statusBannerText = "Parking Slot unlocked!",
      )
    }
  }

  fun showStatusNotice(message: String) {
    soundEngine.playBonk()
    statusBannerTimer = 1.6f
    _uiState.update { it.copy(statusBannerText = message) }
  }

  fun onBoosterTapped(type: BoosterType) {
    val state = _uiState.value
    if (state.outcome != GameOutcome.PLAYING) return
    soundEngine.playClick()

    val owned =
      when (type) {
        BoosterType.BOARD -> state.boardBoosters
        BoosterType.REFRESH -> state.refreshBoosters
        BoosterType.VIP_SLOT -> state.vipBoosters
      }

    if (type == BoosterType.BOARD && state.showBoardBoosterHand) {
      _uiState.update { it.copy(showBoardBoosterHand = false) }
    }

    if (owned <= 0) {
      _uiState.update { it.copy(activeBoosterShop = type, shopNotice = null) }
      return
    }

    executeBooster(type)
  }

  fun buyAndUseBooster(type: BoosterType) {
    val state = _uiState.value
    if (state.coins < type.costCoins) {
      soundEngine.playBonk()
      return
    }

    soundEngine.playClick()
    soundEngine.playDing()
    viewModelScope.launch {
      prefsRepo.adjustCoins(-type.costCoins)
      when (type) {
        BoosterType.BOARD -> prefsRepo.setBoosterCount(board = _progress.value.boardBoosters + 1)
        BoosterType.REFRESH -> prefsRepo.setBoosterCount(refresh = _progress.value.refreshBoosters + 1)
        BoosterType.VIP_SLOT -> prefsRepo.setBoosterCount(vip = _progress.value.vipBoosters + 1)
      }
    }
    _uiState.update { it.copy(activeBoosterShop = null, shopNotice = null) }
  }

  fun claimFreeBooster(type: BoosterType) {
    soundEngine.playClick()
    soundEngine.playUnlock()
    viewModelScope.launch {
      when (type) {
        BoosterType.BOARD -> prefsRepo.setBoosterCount(board = _progress.value.boardBoosters + 1)
        BoosterType.REFRESH -> prefsRepo.setBoosterCount(refresh = _progress.value.refreshBoosters + 1)
        BoosterType.VIP_SLOT -> prefsRepo.setBoosterCount(vip = _progress.value.vipBoosters + 1)
      }
    }
    statusBannerTimer = 1.6f
    _uiState.update {
      it.copy(
        activeBoosterShop = null,
        shopNotice = null,
        statusBannerText = "+1 ${type.title} added!",
      )
    }
  }

  fun buyBoosterFromShop(type: BoosterType) {
    val state = _uiState.value
    if (state.coins < type.costCoins) {
      soundEngine.playBonk()
      _uiState.update { it.copy(shopNotice = "Not enough coins! Need ${type.costCoins} coins.") }
      return
    }
    soundEngine.playClick()
    soundEngine.playDing()
    viewModelScope.launch {
      prefsRepo.adjustCoins(-type.costCoins)
      when (type) {
        BoosterType.BOARD -> prefsRepo.setBoosterCount(board = _progress.value.boardBoosters + 1)
        BoosterType.REFRESH -> prefsRepo.setBoosterCount(refresh = _progress.value.refreshBoosters + 1)
        BoosterType.VIP_SLOT -> prefsRepo.setBoosterCount(vip = _progress.value.vipBoosters + 1)
      }
    }
    _uiState.update { it.copy(shopNotice = "Purchased +1 ${type.title}!") }
  }

  private fun executeBooster(type: BoosterType) {
    val state = _uiState.value
    when (type) {
      BoosterType.BOARD -> {
        var workingVehicles = state.vehicles.toMutableList()
        var workingSlots = state.slots.toMutableList()

        var bufferedBuses =
          workingVehicles
            .filter {
              (it.stage == VehicleStage.PARKED || it.stage == VehicleStage.DRIVING_TO_SLOT) &&
                it.reservedCount < it.capacity
            }
            .sortedBy { it.slotIndex }

        if (bufferedBuses.isEmpty()) {
          val freeSlotIdx = workingSlots.indexOfFirst { it.unlocked && it.occupantVehicleId == null }
          val lotVehicles = workingVehicles.filter { it.stage == VehicleStage.IN_LOT }
          val ringColors =
            state.passengers
              .asSequence()
              .filter { it.segment == ConveyorSegment.RING }
              .map { it.color }
              .toSet()
          val trackColors = state.passengers.asSequence().map { it.color }.toSet()
          val autoPick =
            lotVehicles.firstOrNull { v ->
              v.color in ringColors && CollisionEngine.checkPath(v, lotVehicles, state.gridCols, state.gridRows).isClear
            }
              ?: lotVehicles.firstOrNull { v ->
                v.color in trackColors && CollisionEngine.checkPath(v, lotVehicles, state.gridCols, state.gridRows).isClear
              }
              ?: lotVehicles.firstOrNull { it.color in ringColors }
              ?: lotVehicles.firstOrNull { it.color in trackColors }

          if (freeSlotIdx >= 0 && autoPick != null) {
            workingSlots[freeSlotIdx] = workingSlots[freeSlotIdx].copy(occupantVehicleId = autoPick.id)
            val pickIdx = workingVehicles.indexOfFirst { it.id == autoPick.id }
            if (pickIdx >= 0) {
              workingVehicles[pickIdx] =
                autoPick.copy(
                  stage = VehicleStage.PARKED,
                  slotIndex = freeSlotIdx,
                  animProgress = 1f,
                  roofOpenProgress = 1f,
                  bounceTimer = 0.24f,
                )
              bufferedBuses = listOf(workingVehicles[pickIdx])
            }
          }
        }

        if (bufferedBuses.isEmpty()) {
          statusBannerTimer = 1.5f
          _uiState.update { it.copy(statusBannerText = "Park a bus in a waiting slot first!") }
          return
        }

        // Activate the primary buffered bus in the waiting slots that has matching passengers on track
        val activeBus =
          bufferedBuses.firstOrNull { bus -> state.passengers.any { it.color == bus.color } }
            ?: bufferedBuses.first()

        val updatedVehicles = workingVehicles
        val vIdx = updatedVehicles.indexOfFirst { it.id == activeBus.id }
        if (vIdx < 0) return

        var bus =
          updatedVehicles[vIdx].copy(
            stage = VehicleStage.PARKED,
            animProgress = 1f,
            roofOpenProgress = 1f,
            bounceTimer = 0.24f,
          )
        val remainingPassengers = state.passengers.toMutableList()
        val newJumps = state.jumpingPassengers.toMutableList()
        var boardedAny = false

        val needed = (bus.capacity - bus.reservedCount).coerceAtLeast(0)
        // Pick matching passengers of the active bus color only (RING first, closest first), leaving all other colors untouched
        val matchedEntries =
          remainingPassengers
            .withIndex()
            .filter { it.value.color == bus.color }
            .sortedWith(
              compareBy<IndexedValue<ConveyorPassenger>>(
                {
                  when (it.value.segment) {
                    ConveyorSegment.RING -> 0
                    ConveyorSegment.LEFT_ARM -> 1
                    ConveyorSegment.RIGHT_ARM -> 2
                  }
                },
                { -it.value.distDp },
              )
            )
            .take(needed)

        if (matchedEntries.isNotEmpty()) {
          val indicesToRemove = matchedEntries.map { it.index }.toHashSet()
          matchedEntries.forEachIndexed { launchIdx, (_, passenger) ->
            bus = bus.copy(reservedCount = bus.reservedCount + 1)
            boardedAny = true
            val (_, posDp) = laneGeometry.passengerCoords(passenger.segment, passenger.distDp, passenger.colInRow)
            newJumps.add(
              JumpingPassenger(
                id = nextEntityId++,
                color = passenger.color,
                targetVehicleId = bus.id,
                targetSlotIndex = bus.slotIndex,
                startNormX = (posDp.first / laneGeometry.widthDp).coerceIn(0.03f, 0.97f),
                startNormY = (posDp.second / laneGeometry.heightDp).coerceIn(0.02f, 0.44f),
                progress = 0f,
                // Fast, crisp staggered flight so counter ticks down smoothly (e.g. 24 -> 0) as each passenger lands
                speed = 1f / (0.16f + launchIdx * 0.014f),
              )
            )
          }
          val filtered = remainingPassengers.filterIndexed { idx, _ -> idx !in indicesToRemove }
          remainingPassengers.clear()
          remainingPassengers.addAll(filtered)
        }

        updatedVehicles[vIdx] = bus

        if (!boardedAny) {
          statusBannerTimer = 1.5f
          _uiState.update { it.copy(statusBannerText = "No matching passengers left on track!") }
          return
        }

        for (k in remainingPassengers.indices) {
          remainingPassengers[k] = remainingPassengers[k].copy(queueIndex = k)
        }
        soundEngine.playWhoosh()
        loseGraceTimer = 0f
        viewModelScope.launch {
          prefsRepo.setBoosterCount(board = (_progress.value.boardBoosters - 1).coerceAtLeast(0))
        }
        _uiState.update {
          it.copy(
            vehicles = updatedVehicles,
            slots = workingSlots,
            passengers = remainingPassengers,
            jumpingPassengers = newJumps,
            boardBoosters = (it.boardBoosters - 1).coerceAtLeast(0),
          )
        }
      }

      BoosterType.REFRESH -> {
        val inLot = state.vehicles.filter { it.stage == VehicleStage.IN_LOT }
        if (inLot.isEmpty()) return

        val reshuffled =
          reshuffleSolvable(
            inLot = inLot,
            tunnels = state.tunnels,
            cols = state.gridCols,
            rows = state.gridRows,
            isOrthographicLot = state.isOrthographicLot,
          )
        val nonLot = state.vehicles.filter { it.stage != VehicleStage.IN_LOT }
        soundEngine.playWhoosh()
        viewModelScope.launch {
          prefsRepo.setBoosterCount(refresh = (_progress.value.refreshBoosters - 1).coerceAtLeast(0))
        }
        val particles = state.particles.toMutableList()
        spawnConfettiBurst(particles, 0.5f, 0.65f, 14)
        _uiState.update {
          it.copy(
            vehicles = nonLot + reshuffled,
            particles = particles,
            tutorialVehicleId =
              if (it.isTutorial) {
                CollisionEngine.firstUnblockedVehicleId(reshuffled, it.gridCols, it.gridRows)
              } else {
                null
              },
          )
        }
      }

      BoosterType.VIP_SLOT -> {
        val targetSlotIdx =
          if (!state.slots[0].unlocked) 0
          else state.slots.indexOfFirst { !it.unlocked }
        if (targetSlotIdx < 0) {
          statusBannerTimer = 1.5f
          _uiState.update { it.copy(statusBannerText = "All waiting slots are already unlocked!") }
          return
        }
        soundEngine.playUnlock()
        viewModelScope.launch {
          prefsRepo.setBoosterCount(vip = (_progress.value.vipBoosters - 1).coerceAtLeast(0))
        }
        val updatedSlots =
          state.slots.mapIndexed { idx, s ->
            if (idx == targetSlotIdx) s.copy(unlocked = true) else s
          }
        _uiState.update { it.copy(slots = updatedSlots) }
      }
    }
  }

  private fun reshuffleSolvable(
    inLot: List<VehicleEntity>,
    tunnels: List<TunnelState>,
    cols: Int,
    rows: Int,
    isOrthographicLot: Boolean = false,
  ): List<VehicleEntity> {
    if (isOrthographicLot || inLot.any { it.direction.isDiagonal }) {
      val rng = Random(System.nanoTime())
      val byCap = inLot.groupBy { it.capacity }
      val recoloredById = mutableMapOf<Int, VehicleColor>()
      for ((_, group) in byCap) {
        val colors = group.map { it.color }.shuffled(rng)
        group.forEachIndexed { idx, v -> recoloredById[v.id] = colors[idx] }
      }
      return inLot.map { v -> v.copy(color = recoloredById[v.id] ?: v.color) }
    }

    val dirs = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)

    val positions = inLot.map { Pair(it.x, it.y) }
    for (attempt in 0 until 80) {
      val rng = Random(System.nanoTime() + attempt)
      val shuffledPos = positions.shuffled(rng)
      val candidateList =
        inLot.mapIndexed { idx, v ->
          val (nx, ny) = shuffledPos[idx]
          v.copy(x = nx, y = ny, direction = dirs[rng.nextInt(dirs.size)])
        }

      var hasOverlap = false
      for (i in candidateList.indices) {
        val boxA = CollisionEngine.boxForVehicle(candidateList[i])
        for (j in i + 1 until candidateList.size) {
          if (CollisionEngine.overlaps(boxA, CollisionEngine.boxForVehicle(candidateList[j]))) {
            hasOverlap = true
            break
          }
        }
        if (hasOverlap) break
      }
      if (hasOverlap) continue

      if (CollisionEngine.canClearAll(candidateList, tunnels, cols, rows)) {
        return candidateList
      }
    }

    // Fallback: orient all remaining vehicles outward cleanly without overlap
    return inLot.map { v ->
      val outDir =
        when {
          v.y <= rows * 0.5f -> Direction.UP
          else -> Direction.DOWN
        }
      v.copy(direction = outDir)
    }
  }

  fun reviveOnLose() {
    val state = _uiState.value
    if (state.coins < 200) {
      soundEngine.playBonk()
      _uiState.update { it.copy(shopNotice = "Need 200 coins to revive!") }
      return
    }
    soundEngine.playDing()
    viewModelScope.launch { prefsRepo.adjustCoins(-200) }
    val lockedIdx = state.slots.indexOfFirst { !it.unlocked }
    val updatedSlots =
      if (lockedIdx >= 0) {
        state.slots.mapIndexed { idx, s -> if (idx == lockedIdx) s.copy(unlocked = true) else s }
      } else {
        state.slots
      }
    // If all 8 bays are already unlocked, instantly depart the highest-progress parked bus to free a bay
    var updatedVehicles = state.vehicles
    var updatedPassengers = state.passengers
    var finalSlots = updatedSlots
    var boardedDelta = 0
    if (lockedIdx < 0) {
      val freeCandidate =
        state.vehicles
          .filter { it.stage == VehicleStage.PARKED }
          .maxByOrNull { it.boardedCount }
      if (freeCandidate != null) {
        val needed = (freeCandidate.capacity - freeCandidate.boardedCount).coerceAtLeast(0)
        val mutablePass = state.passengers.toMutableList()
        repeat(needed) {
          val mIdx = mutablePass.indexOfFirst { it.color == freeCandidate.color }
          if (mIdx >= 0) {
            mutablePass.removeAt(mIdx)
            boardedDelta++
          }
        }
        updatedPassengers = mutablePass.mapIndexed { idx, p -> p.copy(queueIndex = idx) }
        updatedVehicles =
          state.vehicles.map { v ->
            if (v.id == freeCandidate.id) {
              v.copy(
                boardedCount = freeCandidate.capacity,
                reservedCount = freeCandidate.capacity,
                stage = VehicleStage.DEPARTING_FULL,
                animProgress = 0f,
              )
            } else {
              v
            }
          }
      }
    }
    loseGraceTimer = 0f
    _uiState.update {
      it.copy(
        slots = finalSlots,
        vehicles = updatedVehicles,
        passengers = updatedPassengers,
        boardedPassengersTotal = it.boardedPassengersTotal + boardedDelta,
        outcome = GameOutcome.PLAYING,
        usedRevive = true,
        shopNotice = null,
      )
    }
  }

  fun pauseGame() {
    soundEngine.playPauseClick()
    if (_uiState.value.outcome == GameOutcome.PLAYING) {
      _uiState.update { it.copy(outcome = GameOutcome.PAUSED, isSpeedBoosted = false) }
    }
  }

  fun resumeGame() {
    soundEngine.playClick()
    if (_uiState.value.outcome == GameOutcome.PAUSED) {
      _uiState.update { it.copy(outcome = GameOutcome.PLAYING) }
    }
  }

  fun restartLevel() {
    soundEngine.playClick()
    startLevel(_uiState.value.levelNumber)
  }

  fun nextLevel() {
    val state = _uiState.value
    soundEngine.playClick()
    if (state.levelNumber == 1 && state.outcome == GameOutcome.WON && !state.showBoardUnlockedPopup) {
      soundEngine.playUnlock()
      _uiState.update { it.copy(showBoardUnlockedPopup = true) }
    } else {
      startLevel(state.levelNumber + 1)
    }
  }

  fun claimWinBonusAndNext(bonusCoins: Int = 400) {
    soundEngine.playClick()
    soundEngine.playUnlock()
    val state = _uiState.value
    viewModelScope.launch { prefsRepo.adjustCoins(bonusCoins) }
    if (state.levelNumber == 1 && state.outcome == GameOutcome.WON && !state.showBoardUnlockedPopup) {
      _uiState.update { it.copy(showBoardUnlockedPopup = true) }
    } else {
      startLevel(state.levelNumber + 1)
    }
  }

  fun confirmBoardUnlockedAndStartNextLevel() {
    soundEngine.playClick()
    soundEngine.playLockOpen()
    val currentBoard = _progress.value.boardBoosters
    if (currentBoard <= 0) {
      viewModelScope.launch { prefsRepo.setBoosterCount(board = 1) }
    }
    val nextLvl = (_uiState.value.levelNumber + 1).coerceAtLeast(2)
    startLevel(nextLvl)

    // Immediately pull a matching clear bus from the grid into the first waiting slot so it stands ready to board
    _uiState.update { state ->
      val freeSlotIdx = state.slots.indexOfFirst { it.unlocked && it.occupantVehicleId == null }
      val lotVehicles = state.vehicles.filter { it.stage == VehicleStage.IN_LOT }
      val ringColors =
        state.passengers
          .asSequence()
          .filter { it.segment == ConveyorSegment.RING }
          .map { it.color }
          .toSet()
      val trackColors = state.passengers.asSequence().map { it.color }.toSet()

      val chosenBus =
        if (freeSlotIdx >= 0) {
          lotVehicles.firstOrNull { v ->
            v.color in ringColors && CollisionEngine.checkPath(v, lotVehicles, state.gridCols, state.gridRows).isClear
          }
            ?: lotVehicles.firstOrNull { v ->
              v.color in trackColors && CollisionEngine.checkPath(v, lotVehicles, state.gridCols, state.gridRows).isClear
            }
            ?: lotVehicles.firstOrNull { it.color in ringColors }
        } else {
          null
        }

      if (freeSlotIdx >= 0 && chosenBus != null) {
        val updatedSlots =
          state.slots.mapIndexed { idx, s ->
            if (idx == freeSlotIdx) s.copy(occupantVehicleId = chosenBus.id) else s
          }
        val updatedVehicles =
          state.vehicles.map { v ->
            if (v.id == chosenBus.id) {
              v.copy(
                stage = VehicleStage.DRIVING_TO_SLOT,
                slotIndex = freeSlotIdx,
                startX = v.x,
                startY = v.y,
                targetX = v.x,
                targetY = v.y,
                animProgress = 0f,
              )
            } else {
              v
            }
          }
        state.copy(
          vehicles = updatedVehicles,
          slots = updatedSlots,
          boardBoosters = state.boardBoosters.coerceAtLeast(1),
          showBoardBoosterHand = true,
        )
      } else {
        state.copy(
          boardBoosters = state.boardBoosters.coerceAtLeast(1),
          showBoardBoosterHand = true,
        )
      }
    }
  }

  fun toggleHelp(show: Boolean) {
    soundEngine.playClick()
    _uiState.update { it.copy(showHelpDialog = show) }
  }

  fun toggleMainShop(show: Boolean) {
    soundEngine.playClick()
    _uiState.update { it.copy(showMainShopDialog = show, shopNotice = null) }
  }

  fun dismissBoosterShop() {
    soundEngine.playClick()
    _uiState.update { it.copy(activeBoosterShop = null, shopNotice = null) }
  }

  fun dismissSlotUnlock() {
    soundEngine.playClick()
    _uiState.update { it.copy(activeSlotUnlockIndex = null, shopNotice = null) }
  }

  fun toggleSound(enabled: Boolean) {
    if (enabled) {
      soundEngine.soundEnabled = true
      soundEngine.playClick()
    } else {
      soundEngine.playClick()
      soundEngine.soundEnabled = false
    }
    viewModelScope.launch { prefsRepo.setSoundEnabled(enabled) }
  }

  fun toggleVibration(enabled: Boolean) {
    soundEngine.playClick()
    viewModelScope.launch { prefsRepo.setVibrationEnabled(enabled) }
  }

  fun toggleCloudSave(enabled: Boolean) {
    soundEngine.playClick()
    viewModelScope.launch { prefsRepo.setCloudSaveEnabled(enabled) }
  }

  fun cycleLanguage() {
    soundEngine.playClick()
    val next = if (_progress.value.languageLabel == "English") "اردو" else "English"
    viewModelScope.launch { prefsRepo.setLanguageLabel(next) }
  }

  fun setPassengerSizeScale(scale: Float) {
    val clamped = scale.coerceIn(0.65f, 1.50f)
    laneGeometry = LaneGeometry(laneGeometry.widthDp, laneGeometry.heightDp, clamped, laneGeometry.trackShape)
    _uiState.update { it.copy(passengerSizeScale = clamped) }
    viewModelScope.launch { prefsRepo.setPassengerSizeScale(clamped) }
  }

  fun setPassengerHeadScale(scale: Float) {
    val clamped = scale.coerceIn(0.65f, 1.65f)
    _uiState.update { it.copy(passengerHeadScale = clamped) }
    viewModelScope.launch { prefsRepo.setPassengerHeadScale(clamped) }
  }

  fun setPassengerWidthScale(scale: Float) {
    val clamped = scale.coerceIn(0.65f, 1.65f)
    _uiState.update { it.copy(passengerWidthScale = clamped) }
    viewModelScope.launch { prefsRepo.setPassengerWidthScale(clamped) }
  }

  fun setPassengerHeightScale(scale: Float) {
    val clamped = scale.coerceIn(0.65f, 1.65f)
    _uiState.update { it.copy(passengerHeightScale = clamped) }
    viewModelScope.launch { prefsRepo.setPassengerHeightScale(clamped) }
  }

  fun resetPassengerTuning() {
    laneGeometry = LaneGeometry(laneGeometry.widthDp, laneGeometry.heightDp, 1.0f, laneGeometry.trackShape)
    _uiState.update {
      it.copy(
        passengerSizeScale = 1.0f,
        passengerHeadScale = 1.0f,
        passengerWidthScale = 1.0f,
        passengerHeightScale = 1.0f,
      )
    }
    viewModelScope.launch { prefsRepo.resetPassengerTuning() }
  }

  fun claimStarterPack() {
    soundEngine.playClick()
    soundEngine.playUnlock()
    viewModelScope.launch { prefsRepo.claimStarterPack() }
    _uiState.update { it.copy(shopNotice = "Starter Pack claimed! +1000 coins & x2 boosters") }
  }

  fun unlockNoAds() {
    soundEngine.playClick()
    soundEngine.playUnlock()
    viewModelScope.launch { prefsRepo.unlockNoAds() }
    _uiState.update { it.copy(shopNotice = "No Ads activated!") }
  }

  fun claimNoAdsPack() {
    soundEngine.playClick()
    soundEngine.playUnlock()
    viewModelScope.launch { prefsRepo.claimNoAdsPack() }
    _uiState.update { it.copy(shopNotice = "No Ads Pack claimed! +1000 coins & x1 boosters") }
  }

  fun claimSmallPack() {
    soundEngine.playClick()
    soundEngine.playUnlock()
    viewModelScope.launch { prefsRepo.claimSmallPack() }
    _uiState.update { it.copy(shopNotice = "Small Pack added! +5000 coins & x1 boosters") }
  }

  fun addBonusCoins(amount: Int) {
    soundEngine.playClick()
    soundEngine.playUnlock()
    viewModelScope.launch { prefsRepo.adjustCoins(amount) }
    _uiState.update { it.copy(shopNotice = "+$amount coins added!") }
  }

  private fun slotNormalizedX(slotIndex: Int, @Suppress("UNUSED_PARAMETER") totalSlots: Int): Float {
    val centers = floatArrayOf(0.1280f, 0.2370f, 0.3360f, 0.4350f, 0.5340f, 0.6340f, 0.7360f, 0.8400f)
    return if (slotIndex in centers.indices) centers[slotIndex]
    else (0.1280f + slotIndex * 0.1010f).coerceIn(0.08f, 0.92f)
  }

  private fun isoGridToPlayfieldNorm(
    gx: Float,
    gy: Float,
    gridCols: Int,
    gridRows: Int,
    isOrthographicLot: Boolean = false,
  ): Pair<Float, Float> {
    val w = laneGeometry.widthDp.coerceAtLeast(320f)
    val h = laneGeometry.heightDp.coerceAtLeast(600f)
    val lotTop = h * 0.452f
    val lotH = h * 0.455f
    val du = gx - gridCols * 0.5f
    val dv = gy - gridRows * 0.5f
    if (isOrthographicLot) {
      val spanX = gridCols.toFloat().coerceAtLeast(4f)
      val spanY = gridRows.toFloat().coerceAtLeast(4f)
      val cellByW = (w * 0.80f) / spanX
      val cellByH = (lotH * 0.84f) / spanY
      val cellDp = min(cellByW, cellByH).coerceIn(14f, 44f)
      val centerX = w * 0.495f
      val centerY = lotTop + lotH * 0.515f
      val sx = centerX + du * cellDp
      val sy = centerY + dv * cellDp
      return (sx / w).coerceIn(0.04f, 0.96f) to (sy / h).coerceIn(0.42f, 0.94f)
    }
    val spanX = (gridCols * IsoProjection.UX + gridRows * IsoProjection.VX).coerceAtLeast(3.5f)
    val spanY = (gridCols * abs(IsoProjection.UY) + gridRows * IsoProjection.VY).coerceAtLeast(3.5f)
    val cellByW = (w * 0.88f) / spanX
    val cellByH = (lotH * 0.90f) / spanY
    val cellDp = min(cellByW, cellByH).coerceIn(18f, 44f)
    val centerX = w * 0.49f
    val centerY = lotTop + lotH * 0.49f
    val sx = centerX + (du * IsoProjection.UX + dv * IsoProjection.VX) * cellDp
    val sy = centerY + (du * IsoProjection.UY + dv * IsoProjection.VY) * cellDp
    return (sx / w).coerceIn(0.04f, 0.96f) to (sy / h).coerceIn(0.42f, 0.94f)
  }

  private fun spawnCollisionEffects(
    list: MutableList<Particle>,
    nx: Float,
    ny: Float,
  ) {
    // 1. Billowing cartoon smoke puffs right at the bumper collision point
    repeat(6) { i ->
      val angle = (i.toFloat() / 6f) * (2f * PI.toFloat()) + Random.nextFloat() * 0.5f
      val speed = 0.04f + Random.nextFloat() * 0.06f
      val puffLife = 0.52f + Random.nextFloat() * 0.16f
      list.add(
        Particle(
          id = nextEntityId++,
          x = nx + (Random.nextFloat() - 0.5f) * 0.025f,
          y = ny + (Random.nextFloat() - 0.5f) * 0.015f,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed * 0.6f - 0.06f,
          color = if (i % 2 == 0) Color(0xFFF3F6FA) else Color(0xFFD0DBE8),
          sizeDp = 22f + Random.nextFloat() * 10f,
          life = puffLife,
          maxLife = puffLife,
          isSmokePuff = true,
        )
      )
    }
    // 2. Crisp impact sparks radiating outward on contact
    val sparkColors = listOf(Color(0xFFFFE042), Color(0xFFFF8C1A), Color.White)
    repeat(8) { i ->
      val angle = (i.toFloat() / 8f) * (2f * PI.toFloat()) + Random.nextFloat() * 0.35f
      val speed = 0.16f + Random.nextFloat() * 0.18f
      val sparkLife = 0.28f + Random.nextFloat() * 0.10f
      list.add(
        Particle(
          id = nextEntityId++,
          x = nx,
          y = ny,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed - 0.06f,
          color = sparkColors[i % sparkColors.size],
          sizeDp = 5.5f + Random.nextFloat() * 3f,
          life = sparkLife,
          maxLife = sparkLife,
          isSpark = true,
        )
      )
    }
  }

  private fun spawnSmokeCloud(
    list: MutableList<Particle>,
    nx: Float,
    ny: Float,
    count: Int,
  ) {
    repeat(count) { i ->
      list.add(
        Particle(
          id = nextEntityId++,
          x = nx - Random.nextFloat() * 0.04f,
          y = ny + (Random.nextFloat() - 0.5f) * 0.025f,
          vx = -0.14f - Random.nextFloat() * 0.16f,
          vy = -0.06f - Random.nextFloat() * 0.08f,
          color = if (i % 2 == 0) Color(0xFFF3F6FA) else Color(0xFFD6E0EC),
          sizeDp = 12f + Random.nextFloat() * 8f,
          life = 0.65f,
          maxLife = 0.65f,
          isConfetti = false,
        )
      )
    }
  }

  private fun spawnConfettiBurst(
    list: MutableList<Particle>,
    nx: Float,
    ny: Float,
    count: Int,
  ) {
    val colors = listOf(GoldCoin, PlayGreen, Color(0xFFFF4655), Color(0xFF16C8E8), Color(0xFF8E44EC))
    repeat(count) { i ->
      val angle = (i.toFloat() / count) * (2f * PI.toFloat()) + Random.nextFloat() * 0.4f
      val speed = 0.18f + Random.nextFloat() * 0.28f
      list.add(
        Particle(
          id = nextEntityId++,
          x = nx,
          y = ny,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed - 0.18f,
          color = colors[i % colors.size],
          sizeDp = 6f + Random.nextFloat() * 4f,
          life = 0.85f,
          maxLife = 0.85f,
          isConfetti = true,
        )
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    soundEngine.release()
  }
}
