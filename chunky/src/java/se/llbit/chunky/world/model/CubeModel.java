/*
 * Copyright (c) 2017 Jesper Öqvist <jesper@llbit.se>
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.llbit.chunky.world.model;

import se.llbit.chunky.resources.Texture;
import se.llbit.chunky.resources.TexturePackLoader;
import se.llbit.chunky.resources.texturepack.SimpleTexture;
import se.llbit.chunky.resources.texturepack.TextureLoader;
import se.llbit.math.Quad;
import se.llbit.math.Ray;
import se.llbit.math.Vector2;
import se.llbit.math.Vector3;
import se.llbit.math.Vector4;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CubeModel {
  public Quad[] quads = new Quad[0];
  public Texture[] textures = new Texture[0];
  private static Map<String, Texture> textureMap = new HashMap<>();

  public CubeModel() {
  }

  public CubeModel(File resourcePack, Collection<Cube> cubes, double uvScale) {
    Collection<Quad> theFaces = new ArrayList<>();
    Collection<String> theTextures = new ArrayList<>();
    for (Cube cube : cubes) {
      if (!cube.visible) {
        continue;
      }
      for (Face face : cube.faces) {
        if (!face.visible) {
          continue;
        }
        Vector3 from = new Vector3(cube.start);
        from.scale(1 / 16.0);
        Vector3 to = new Vector3(cube.end);
        to.scale(1 / 16.0);
        Vector2 uv0 = new Vector2(face.uv0);
        Vector2 uv1 = new Vector2(face.uv1);
        Vector4 uv = new Vector4(uv0.x, uv1.x, uvScale - uv1.y, uvScale - uv0.y);
        uv.scale(1 / uvScale);
        theTextures.add(face.texture);
        double[][] fromto = {
            { from.x, from.y, from.z },
            { to.x, to.y, to.z },
        };
        switch (face.name) {
          case "up":
            // passive = { 1, 0 };
            // x0 = 0 : 0 -> 1;
            // x : { { 0, 0 }, { 0, 1 } }
            //     { { component, from/to } }
            // y = 2 : 1 -> 0;
            // y : { { 2, 1 }, { 2, 0 } }
            addRot(theFaces, face,
                fromto,
                new int[] { 1, 1 },
                new int[][] { { 0, 0 }, { 0, 1 } },
                new int[][] { { 2, 1 }, { 2, 0 } },
                uv, uvScale);
            break;
          case "down":
            addRot(theFaces, face,
                fromto,
                new int[] { 1, 0 },
                new int[][] { { 0, 0 }, { 0, 1 } },
                new int[][] { { 2, 0 }, { 2, 1 } },
                uv, uvScale);
            break;
          case "north":
            addRot(theFaces, face,
                fromto,
                new int[] { 2, 0 },
                new int[][] { { 0, 1 }, { 0, 0 } },
                new int[][] { { 1, 0 }, { 1, 1 } },
                uv, uvScale);
            break;
          case "south":
            addRot(theFaces, face,
                fromto,
                new int[] { 2, 1 },
                new int[][] { { 0, 0 }, { 0, 1 } },
                new int[][] { { 1, 0 }, { 1, 1 } },
                uv, uvScale);
            break;
          case "east":
            addRot(theFaces, face,
                fromto,
                new int[] { 0, 1 },
                new int[][] { { 2, 1 }, { 2, 0 } },
                new int[][] { { 1, 0 }, { 1, 1 } },
                uv, uvScale);
            break;
          case "west":
            addRot(theFaces, face,
                fromto,
                new int[] { 0, 0 },
                new int[][] { { 2, 0 }, { 2, 1 } },
                new int[][] { { 1, 0 }, { 1, 1 } },
                uv, uvScale);
            break;
        }
      }
    }

    Map<String, TextureLoader> toLoad = new HashMap<>();
    for (String id : theTextures) {
      if (!textureMap.containsKey(id)) {
        Texture texture = new Texture();
        if (id.startsWith("#")) {
          System.out.println("Unknown texture: " + id);
        } else {
          toLoad.put(id, new SimpleTexture("assets/minecraft/textures/" + id, texture));
        }
        textureMap.put(id, texture);
      }
    }
    if (!toLoad.isEmpty()) {
      System.out.println("Loading textures: " + toLoad.keySet());
      Set<Map.Entry<String, TextureLoader>> missing =
          TexturePackLoader.loadTextures(resourcePack, toLoad.entrySet());
      for (Map.Entry<String, TextureLoader> tex : missing) {
        System.err.println("Failed to load texture: " + tex.getValue());
      }
    }
    this.textures = new Texture[theTextures.size()];
    int index = 0;
    for (String id : theTextures) {
      //this.textures[index++] = Texture.stone;
      this.textures[index++] = textureMap.get(id);
    }
    quads = new Quad[theFaces.size()];
    theFaces.toArray(quads);
  }

  private void addRot(Collection<Quad> theFaces, Face face,
      double[][] fromto,
      int[] passive,
      int[][] x,
      int[][] y,
      Vector4 uv, double uvScale) {
    double[] o = new double[3];
    double[] u = new double[3];
    double[] v = new double[3];
    // Encoding: { { component, from/to } }
    // component : x=0, y=1, z=2
    // from = 0, to = 1
    switch (face.rotation) {
      case 0:
        // x0y0
        o[x[0][0]] = fromto[x[0][1]][x[0][0]];
        o[passive[0]] = fromto[passive[1]][passive[0]];
        o[y[0][0]] = fromto[y[0][1]][y[0][0]];

        // u: x1y0
        u[x[0][0]] = fromto[x[1][1]][x[0][0]];
        u[passive[0]] = fromto[passive[1]][passive[0]];
        u[y[0][0]] = fromto[y[0][1]][y[0][0]];

        // v: x0y1
        v[x[0][0]] = fromto[x[0][1]][x[0][0]];
        v[passive[0]] = fromto[passive[1]][passive[0]];
        v[y[0][0]] = fromto[y[1][1]][y[0][0]];
        break;
      case 1:
        // 90 degrees.
        // x0,y1
        o[x[0][0]] = fromto[x[0][1]][x[0][0]];
        o[passive[0]] = fromto[passive[1]][passive[0]];
        o[y[0][0]] = fromto[y[1][1]][y[0][0]];

        // x0,y0
        u[x[0][0]] = fromto[x[0][1]][x[0][0]];
        u[passive[0]] = fromto[passive[1]][passive[0]];
        u[y[0][0]] = fromto[y[0][1]][y[0][0]];

        // x1,y1
        v[x[0][0]] = fromto[x[1][1]][x[0][0]];
        v[passive[0]] = fromto[passive[1]][passive[0]];
        v[y[0][0]] = fromto[y[1][1]][y[0][0]];
        break;
      case 2:
        // x1,y1
        o[x[0][0]] = fromto[x[1][1]][x[0][0]];
        o[passive[0]] = fromto[passive[1]][passive[0]];
        o[y[0][0]] = fromto[y[1][1]][y[0][0]];

        // x0,y1
        u[x[0][0]] = fromto[x[0][1]][x[0][0]];
        u[passive[0]] = fromto[passive[1]][passive[0]];
        u[y[0][0]] = fromto[y[1][1]][y[0][0]];

        // x1,y0
        v[x[0][0]] = fromto[x[1][1]][x[0][0]];
        v[passive[0]] = fromto[passive[1]][passive[0]];
        v[y[0][0]] = fromto[y[0][1]][y[0][0]];
        break;
      case 3:
        // 270 degrees.
        // x1,y0
        o[x[0][0]] = fromto[x[1][1]][x[0][0]];
        o[passive[0]] = fromto[passive[1]][passive[0]];
        o[y[0][0]] = fromto[y[0][1]][y[0][0]];

        // x1,y1
        u[x[0][0]] = fromto[x[1][1]][x[0][0]];
        u[passive[0]] = fromto[passive[1]][passive[0]];
        u[y[0][0]] = fromto[y[1][1]][y[0][0]];

        // x0,y0
        v[x[0][0]] = fromto[x[0][1]][x[0][0]];
        v[passive[0]] = fromto[passive[1]][passive[0]];
        v[y[0][0]] = fromto[y[0][1]][y[0][0]];
        break;
    }
    addQuad(theFaces,
        new Vector3(o[0], o[1], o[2]),
        new Vector3(u[0], u[1], u[2]),
        new Vector3(v[0], v[1], v[2]),
        uv, uvScale);
  }

  private void addRot(Collection<Quad> theFaces, Face face,
      Vector3 from, Vector3 to,
      Vector4 uv, double uvScale) {
    switch (face.rotation) {
      case 0:
        addQuad(theFaces,
            new Vector3(from.x, to.y, to.z),
            new Vector3(to.x,   to.y, to.z),
            new Vector3(from.x, to.y, from.z),
            uv, uvScale);
        break;
      case 1:
        // 90 deg rot:
        addQuad(theFaces,
            new Vector3(from.z, to.y, from.x),
            new Vector3(from.x, to.y, to.x),
            new Vector3(to.z,   to.y, from.x), uv,
            uvScale);
        break;
      case 2:
        // 180 deg rot:
        addQuad(theFaces,
            new Vector3(to.z,   to.y, from.x),
            new Vector3(from.x, to.y, from.x),
            new Vector3(to.z,   to.y, to.x), uv,
            uvScale);
        break;
      case 3:
        // 270 deg rot:
        addQuad(theFaces,
            new Vector3(to.x,   to.y, to.z),
            new Vector3(to.x,   to.y, from.z),
            new Vector3(from.x, to.y, to.z), uv,
            uvScale);
        break;
    }
  }
  private void addRot2(Collection<Quad> theFaces, Face face,
      double[][] fromto,
      int[] passive,
      int[][] x,
      int[][] y,
      Vector4 uv, double uvScale) {
    double[] o = new double[3];
    double[] u = new double[3];
    double[] v = new double[3];
    // Encoding: { { component, from/to } }
    // component : x=0, y=1, z=2
    // from = 0, to = 1
    switch (face.rotation) {
      case 0:
        // x0y0
        o[x[0][0]] = fromto[x[0][1]][x[0][0]];
        o[passive[0]] = fromto[passive[1]][passive[0]];
        o[y[0][0]] = fromto[y[0][1]][y[0][0]];

        // u: x1y0
        u[x[0][0]] = fromto[x[1][1]][x[0][0]];
        u[passive[0]] = fromto[passive[1]][passive[0]];
        u[y[0][0]] = fromto[y[0][1]][y[0][0]];

        // v: x0y1
        v[x[0][0]] = fromto[x[0][1]][x[0][0]];
        v[passive[0]] = fromto[passive[1]][passive[0]];
        v[y[0][0]] = fromto[y[1][1]][y[0][0]];
        break;
      case 1:
        // y1x0
        o[x[0][0]] = fromto[y[1][1]][y[0][0]];
        o[passive[0]] = fromto[passive[1]][passive[0]];
        o[y[0][0]] = fromto[x[0][1]][x[0][0]];

        // y1x1
        u[x[0][0]] = fromto[y[1][1]][y[0][0]];
        u[passive[0]] = fromto[passive[1]][passive[0]];
        u[y[0][0]] = fromto[x[1][1]][x[0][0]];

        // y0x0
        v[x[0][0]] = fromto[y[0][1]][y[0][0]];
        v[passive[0]] = fromto[passive[1]][passive[0]];
        v[y[0][0]] = fromto[x[0][1]][x[0][0]];
        break;
      case 2:
        // o: (y0, x0)
        o[x[0][0]] = fromto[y[0][1]][y[0][0]];
        o[passive[0]] = fromto[passive[1]][passive[0]];
        o[y[0][0]] = fromto[x[0][1]][x[0][0]];

        // u: (y1, x0)
        u[x[0][0]] = fromto[y[1][1]][y[0][0]];
        u[passive[0]] = fromto[passive[1]][passive[0]];
        u[y[0][0]] = fromto[x[0][1]][x[0][0]];

        // v: (y0, x1)
        v[x[0][0]] = fromto[y[0][1]][y[0][0]];
        v[passive[0]] = fromto[passive[1]][passive[0]];
        v[y[0][0]] = fromto[x[1][1]][x[0][0]];
        break;
      case 3:
        // 270 deg rot:
        o[x[0][0]] = fromto[x[1][1]][x[0][0]];
        o[passive[0]] = fromto[passive[1]][passive[0]];
        o[y[0][0]] = fromto[y[0][1]][y[0][0]];

        u[x[0][0]] = fromto[x[1][1]][x[0][0]];
        u[passive[0]] = fromto[passive[1]][passive[0]];
        u[y[0][0]] = fromto[y[1][1]][y[0][0]];

        v[x[0][0]] = fromto[x[0][1]][x[0][0]];
        v[passive[0]] = fromto[passive[1]][passive[0]];
        v[y[0][0]] = fromto[y[0][1]][y[0][0]];
        break;
    }
    addQuad(theFaces,
        new Vector3(o[0], o[1], o[2]),
        new Vector3(u[0], u[1], u[2]),
        new Vector3(v[0], v[1], v[2]),
        uv, uvScale);
  }

  private void addQuad(Collection<Quad> theFaces, Vector3 x0y0, Vector3 x1y0, Vector3 x0y1,
      Vector4 uv, double uvScale) {
    theFaces.add(new Quad(x0y0, x1y0, x0y1, uv));
  }

  private String formatDouble(double value) {
    if (value == 0) {
      return "0";
    }
    if (value == (int) value) {
      return String.valueOf((int) value);
    } else {
      return String.valueOf(value);
    }
  }

  private String formatDouble(double value, double scale) {
    double v = value * scale;
    if (v == 0) {
      return "0";
    }
    if (v == (int) v) {
      return String.valueOf((int) v) + " / " + String.valueOf(scale);
    } else {
      return String.valueOf(v) + " / " + String.valueOf(scale);
    }
  }
}
