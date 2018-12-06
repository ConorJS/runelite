package net.runelite.client.plugins.cerbpredict;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;

import lombok.AllArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.GraphicID;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Perspective;
import static net.runelite.api.Perspective.LOCAL_TILE_SIZE;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;


public class CerbPredictLavaOverlay extends Overlay
{
	private final Client client;
	private final CerbPredictConfig config;
	private final CerbPredictPlugin plugin;

	@Inject
	public CerbPredictLavaOverlay(Client client, CerbPredictConfig config, CerbPredictPlugin plugin)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (GraphicsObject graphicsObject : client.getGraphicsObjects())
		{
			Color color = config.lavaColour();
			if (graphicsObject.getId() == GraphicID.LAVA_PILLAR) {

				// player, lava pillar
				LocalPoint lavaPillarTile = graphicsObject.getLocation();
				XY<Double> distance_xy =
						distanceBetweenTiles(lavaPillarTile, client.getLocalPlayer().getLocalLocation());

				// draw lava pillar aoe
				Polygon poly = Perspective.getCanvasTileAreaPoly(client, lavaPillarTile, 3);

				if ((distance_xy.x() < 1) & (distance_xy.y() < 1))
				{
					// red if taking 15's
					color = config.lavaFatalColour();
				}
				else if (distance_xy.x() < 2 & distance_xy.y() < 2)
				{
					// orange if taking 7's
					color = config.lavaDangerColour();
				}

				OverlayUtil.renderPolygon(graphics, poly, color);

				// could draw timer above the lava, but doesn't really matter
			}
		}

		return null;
	}

	private XY<Double> distanceBetweenTiles(LocalPoint localPoint1, LocalPoint localPoint2)
    {
		int x1Loc = localPoint1.getX();
		int y1Loc = localPoint1.getY();
		int x2Loc = localPoint2.getX();
		int y2Loc = localPoint2.getY();

        // LOCAL_TILE_SIZE = size of a tile in local coordinates (128)
		double distance_x = Math.abs((x1Loc - x2Loc) / LOCAL_TILE_SIZE);
		double distance_y = Math.abs((y1Loc - y2Loc) / LOCAL_TILE_SIZE);

		return new XY<Double>(distance_x, distance_y);
	}

	@AllArgsConstructor
	class XY<T>
    {
		private T x;
		private T y;

		public T x() { return x; }
		public T y() { return y; }
	}
}






