package com.tomgibara.grille;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.tomgibara.bits.BitVector;

public class App {

	private static final int CODE_HELP     = 1;
	private static final int CODE_PARAMS   = 2;
	private static final int CODE_SETTINGS = 3;
	private static final int CODE_IMAGE    = 4;
	
	private static final int REVISION = 1;

	private static Settings settings;
	
	public static void main(String... argv) {
		Args args = new Args();
		JCommander jc;

		try {
			jc = new JCommander(args, argv);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			System.exit(CODE_PARAMS);
			return;
		}

		if (args.help) {
			jc.usage();
			System.exit(CODE_HELP);
			return;
		}

		try {
			settings = args.settingsPath == null ? new Settings() : new Settings(args.settingsPath);
		} catch (IOException e) {
			System.err.println("Failed read settings: " + e.getMessage());
			System.exit(CODE_SETTINGS);
			return;
		}

		try {
			generateGrille(args.order, args.seed, args.filename);
		} catch (IOException e) {
			System.err.println("Failed to write grille image: " + e.getMessage());
			System.exit(CODE_IMAGE);
			return;
		}

		System.exit(0);
	}
	
	private static void generateGrille(int order, Long seed, String filename) throws IOException {
		if (seed == null) seed = chooseSeed(order);
		BitVector grille = createGrille(order, seed);
		char revision = 64 + REVISION;
		String title = String.format("%s %010d (%d)", revision, seed, score(grille));
		BufferedImage image = render(grille, title);
		File dir = settings.outputDir;
		dir.mkdirs();
		if (filename == null || filename.isEmpty()) {
			filename = String.format("%s_%d_%010d.png", settings.filename, order, seed);
		}
		File file = new File(dir,  filename);
		ImageIO.write(image, "PNG", file);
	}
	
	private static BufferedImage render(BitVector grille, String title) {
		double pxPerMm = settings.dpi / 27.1;

		int order = order(grille);
		int side = 2 * order;
		int border = 1;
		int sqSide = side + 2 * border;
		double scale = settings.mmPerSquare * pxPerMm;
		int size = (int) (sqSide * scale);
		double radius = 0.45;
		double inset = (1 - radius) / 2;
		
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, size, size);
		
		g.scale(scale, scale);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(0.1f));
		g.drawRect(0, 0, sqSide, sqSide);
		
		g.setStroke(new BasicStroke(0.02f));
		for (int i = 1; i < sqSide; i++) {
			g.drawLine(i, border, i, sqSide - border);
			g.drawLine(border, i, sqSide - border, i);
		}
		
		List<Shape> shapes = new ArrayList<Shape>();
		for (int y = 0; y < side; y++) {
			for (int x = 0; x < side; x++) {
				if (grille.getBit(y * side + x)) {
					Shape shape;
					switch (settings.design) {
					case CIRCLE: 
						shape = new Ellipse2D.Double(border + x + inset, border + y + inset, radius, radius);
						break;
					case SQUARE:
						shape = new Rectangle2D.Double(border + x, border + y, 1, 1);
						break;
						default: throw new IllegalStateException();
					}
					shapes.add(shape);
				}
			}
		}
		Area area = new Area();
		for (Shape shape : shapes) {
			area.add(new Area(shape));
		}

		if (!area.isEmpty()) {
			g.setColor(settings.shaded ? Color.LIGHT_GRAY : Color.WHITE);
			g.fill(area);
			g.setColor(Color.BLACK);
			if (settings.design == Design.SQUARE) {
				g.setStroke(new BasicStroke(0.08f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			}
			g.draw(area);
		}
		
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 6);
		g.setFont(font.deriveFont(AffineTransform.getScaleInstance(1 / pxPerMm, 1 / pxPerMm)));
		Rectangle2D bounds = g.getFontMetrics().getStringBounds(title, g);
		g.drawString(title, 1, (float) ((bounds.getHeight() + 1) / 2.0));
		
		g.dispose();
		return image;
	}
	
	private static long chooseSeed(int order) throws IOException {
		Random r = new Random();
		int bestScore = Integer.MAX_VALUE;
		long bestSeed = 0L;
		for (int i = 0; i < settings.attempts; i++) {
			long seed = r.nextInt() & 0x7fffffff;
			BitVector grille = createGrille(order, seed);
			int score = score(grille);
			if (score < bestScore) {
				bestScore = score;
				bestSeed = seed;
				if (score == 0) break;
			}
		}
		return bestSeed;
	}
	
	private static BitVector createGrille(int order, long seed) {
		int quadrant = order * order;
		int len = (quadrant * 2 + 7) / 8;
		byte[] bytes = new byte[len];
		Random random = new Random(seed);
		random.nextBytes(bytes);
		int bitLen = len * 8;
		BitVector src = new BitVector(bitLen);
		src.setBytes(0, bytes, 0, bitLen);
		
		int size = 2 * order;
		BitVector result = new BitVector(size * size);
		for (int y = 0; y < order; y++) {
			for (int x = 0; x < order; x++) {
				int i = y * order + x;
				int quad = (int) src.getBits(i * 2, 2);
				int rx;
				int ry;
				int s = size - 1;
				switch (quad) {
				case 0: rx = x    ; ry = y    ; break;
				case 1: rx = s - y; ry = x    ; break;
				case 2: rx = s - x; ry = s - y; break;
				case 3: rx = y    ; ry = s - x; break;
				default: continue;
				}
				result.flipBit(ry * size + rx);
			}
		}
		return result;
	}
	
	private static int score(BitVector grille) {
		int order = order(grille);
		int size = order * 2;
		int exp = order * order / 4;
		int score = 0;
		int[] quad = new int[4];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int i = y * size + x;
				if (!grille.getBit(i)) continue;
				if (x > 0 && grille.getBit(i -    1)) score ++;
				if (y > 0 && grille.getBit(i - size)) score ++;
				int q = 0;
				if (x >= order) q += 1;
				if (y >= order) q += 2;
				quad[q]++;
			}
		}
		
		for (int i = 0; i < 4; i++) {
			int q = quad[i];
			if (q != exp) score += Math.abs(q - exp);
		}
		if ((order & 1) == 1) score --;
		if (!connected(grille, size)) score += order * 2;
		return score;
	}
	
	private static boolean connected(BitVector grille, int size) {
		BitVector visited = grille.mutableCopy();
		for (int x = 0; x < size; x++) {
			walk(visited, size, x,        0);
			walk(visited, size, x, size - 1);
		}
		for (int y = 0; y < size; y++) {
			walk(visited, size, 0       , y);
			walk(visited, size, size - 1, y);
		}
		return visited.isAllOnes();
	}
	
	private static void walk(BitVector visited, int size, int x, int y) {
		int i = size * y + x;
		if (visited.getBit(i)) return; // already visited (or a gap)
		visited.setBit(i, true); // mark our visit
		// now walk
		if (x > 0       ) walk(visited, size, x - 1, y    );
		if (x < size - 1) walk(visited, size, x + 1, y    );
		if (y > 0       ) walk(visited, size, x    , y - 1);
		if (y < size - 1) walk(visited, size, x    , y + 1);
	}
	
	private static int order(BitVector grille) {
		return ((int) Math.sqrt( grille.size() )) / 2;
	}
}
