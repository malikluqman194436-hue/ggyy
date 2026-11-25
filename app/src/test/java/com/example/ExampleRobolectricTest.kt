package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.BusView
import com.example.game.CollisionEngine
import com.example.game.IsoBusSpriteFactory
import com.example.game.IsoProjection
import com.example.game.LevelRepository
import com.example.game.VehicleColor
import com.example.game.VehicleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bus Jam", appName)
  }

  @Test
  fun `level 1 diamond buses are non-overlapping and solvable`() {
    val lvl = LevelRepository.getLevel(1)
    assertEquals(14, lvl.vehicles.size)
    val entities =
      lvl.vehicles.mapIndexed { idx, spec ->
        VehicleEntity(
          id = idx + 1,
          color = spec.color,
          direction = spec.direction,
          capacity = spec.capacity,
          x = spec.x,
          y = spec.y,
        )
      }
    for (i in entities.indices) {
      for (j in i + 1 until entities.size) {
        assertFalse(
          "Buses $i and $j overlap",
          CollisionEngine.overlaps(
            CollisionEngine.boxForVehicle(entities[i]),
            CollisionEngine.boxForVehicle(entities[j]),
          ),
        )
      }
    }
    assertTrue("Level 1 must be solvable", CollisionEngine.canClearAll(entities, emptyList(), lvl.gridCols, lvl.gridRows))
    assertEquals(1, CollisionEngine.firstUnblockedVehicleId(entities, lvl.gridCols, lvl.gridRows))
  }

  @Test
  fun `iso bus sprites render cleanly for all primary colors and directions`() {
    val views = listOf(BusView.ISO_1, BusView.ISO_2, BusView.ISO_3, BusView.ISO_4)
    val colors = listOf(VehicleColor.RED, VehicleColor.BLUE, VehicleColor.YELLOW)
    for (c in colors) {
      for (v in views) {
        val img = IsoBusSpriteFactory.renderIsoBus(c, v)
        assertEquals(IsoProjection.SPRITE_SIZE_PX, img.width)
        assertEquals(IsoProjection.SPRITE_SIZE_PX, img.height)
      }
    }
  }
}
