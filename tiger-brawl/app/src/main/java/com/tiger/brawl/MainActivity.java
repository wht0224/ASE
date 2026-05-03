package com.tiger.brawl;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
    private GameView gameView;
    private GameThread gameThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        gameView = new GameView(this);
        setContentView(gameView);
        gameThread = new GameThread(gameView);
        gameThread.start();
    }

    @Override
    protected void onPause() { super.onPause(); gameThread.pauseGame(); }
    @Override
    protected void onResume() { super.onResume(); gameThread.resumeGame(); }

    static class GameThread extends Thread {
        private GameView view;
        private boolean running = true, paused = false;
        GameThread(GameView view) { this.view = view; }
        public void pauseGame() { paused = true; }
        public void resumeGame() { paused = false; }
        @Override
        public void run() {
            long lastTime = System.currentTimeMillis();
            while (running) {
                if (!paused) {
                    long currentTime = System.currentTimeMillis();
                    float dt = (currentTime - lastTime) / 1000.0f;
                    lastTime = currentTime;
                    view.update(dt);
                    view.postInvalidate();
                }
                try { sleep(16); } catch (InterruptedException e) {}
            }
        }
    }

    static class GameView extends View {
        private Paint paint;
        private Player player;
        private List<Enemy> enemies = new ArrayList<>();
        private List<Projectile> projectiles = new ArrayList<>();
        private List<float[]> particles = new ArrayList<>();
        private Random random = new Random();
        private float touchX, touchY;
        private boolean touching = false;
        private float spawnTimer = 0;
        private int wave = 1, killCount = 0, score = 0, combo = 0;
        private long gameTime = 0;
        private boolean gameOver = false;

        public GameView(Context ctx) {
            super(ctx);
            paint = new Paint();
            player = new Player(540, 1200);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawColor(Color.rgb(15, 15, 30));
            
            // Draw player (tiger)
            paint.setColor(Color.argb(80, 255, 180, 80));
            c.drawCircle(player.x, player.y, 50, paint);
            paint.setColor(Color.rgb(255, 180, 80));
            c.drawCircle(player.x, player.y, 40, paint);
            paint.setColor(Color.rgb(255, 140, 40));
            c.drawCircle(player.x - 20, player.y - 20, 15, paint);
            c.drawCircle(player.x + 20, player.y - 20, 15, paint);
            paint.setColor(Color.WHITE);
            c.drawCircle(player.x - 10, player.y - 5, 8, paint);
            c.drawCircle(player.x + 10, player.y - 5, 8, paint);
            paint.setColor(Color.BLACK);
            c.drawCircle(player.x - 10, player.y - 5, 4, paint);
            c.drawCircle(player.x + 10, player.y - 5, 4, paint);

            // Draw enemies
            for (Enemy e : enemies) {
                paint.setColor(Color.argb(60, 255, 60, 60));
                c.drawCircle(e.x, e.y, e.size + 10, paint);
                paint.setColor(e.color);
                c.drawCircle(e.x, e.y, e.size, paint);
                if (e.health < e.maxHealth) {
                    paint.setColor(Color.GRAY);
                    c.drawRect(e.x - 20, e.y - e.size - 10, e.x + 20, e.y - e.size - 5, paint);
                    paint.setColor(Color.GREEN);
                    c.drawRect(e.x - 20, e.y - e.size - 10, e.x - 20 + 40 * e.health / e.maxHealth, e.y - e.size - 5, paint);
                }
            }

            // Draw projectiles
            paint.setColor(Color.YELLOW);
            for (Projectile p : projectiles) c.drawCircle(p.x, p.y, 10, paint);

            // Draw particles
            for (float[] p : particles) {
                paint.setColor(Color.argb((int)(p[4] * 255), (int)p[5], (int)p[6], (int)p[7]));
                c.drawCircle(p[0], p[1], p[3], paint);
            }

            // UI
            paint.setColor(Color.argb(180, 0, 0, 0));
            c.drawRect(0, 0, getWidth(), 100, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(35);
            c.drawText("分数:" + score + "  击杀:" + killCount + "  波次:" + wave, 20, 60, paint);

            // Health bar
            paint.setColor(Color.GRAY);
            c.drawRoundRect(new RectF(20, getHeight()-50, 320, getHeight()-20), 10, 10, paint);
            paint.setColor(Color.GREEN);
            c.drawRoundRect(new RectF(20, getHeight()-50, 20 + 300 * player.health / player.maxHealth, getHeight()-20), 10, 10, paint);

            if (gameOver) {
                paint.setColor(Color.argb(180, 0, 0, 0));
                c.drawRect(0, 0, getWidth(), getHeight(), paint);
                paint.setColor(Color.RED);
                paint.setTextSize(80);
                c.drawText("游戏结束", getWidth()/2 - 160, getHeight()/2, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(40);
                c.drawText("最终分数: " + score, getWidth()/2 - 100, getHeight()/2 + 60, paint);
                c.drawText("点击重开", getWidth()/2 - 80, getHeight()/2 + 120, paint);
            }
        }

        public void update(float dt) {
            if (gameOver) { if (touching) restart(); return; }
            gameTime += dt * 1000;

            if (touching) {
                float dx = touchX - player.x, dy = touchY - player.y;
                float d = (float)Math.sqrt(dx*dx + dy*dy);
                if (d > 10) { player.x += dx/d * player.speed * dt; player.y += dy/d * player.speed * dt; }
            }
            player.x = Math.max(40, Math.min(getWidth()-40, player.x));
            player.y = Math.max(40, Math.min(getHeight()-40, player.y));

            spawnTimer += dt;
            if (spawnTimer > Math.max(0.3f, 2 - wave * 0.1f)) { spawnTimer = 0; spawnEnemy(); }

            for (int i = projectiles.size()-1; i >= 0; i--) {
                Projectile p = projectiles.get(i);
                p.x += p.vx * dt; p.y += p.vy * dt; p.life -= dt;
                if (p.life <= 0 || p.x < -50 || p.x > getWidth()+50 || p.y < -50 || p.y > getHeight()+50) { projectiles.remove(i); continue; }
                for (int j = enemies.size()-1; j >= 0; j--) {
                    Enemy e = enemies.get(j);
                    if (Math.sqrt(Math.pow(p.x-e.x,2) + Math.pow(p.y-e.y,2)) < e.size + 10) {
                        e.health -= p.damage; projectiles.remove(i);
                        for (int k = 0; k < 8; k++) addParticle(p.x, p.y, Color.rgb(255, 100, 50));
                        if (e.health <= 0) { enemies.remove(j); killCount++; score += 100 + combo * 20; combo++;
                            if (killCount % 15 == 0) wave++; }
                        break;
                    }
                }
            }

            for (Enemy e : enemies) {
                float dx = player.x - e.x, dy = player.y - e.y;
                float d = (float)Math.sqrt(dx*dx + dy*dy);
                if (d > 0) { e.x += dx/d * e.speed * dt; e.y += dy/d * e.speed * dt; }
                if (d < e.size + 40) { player.health -= e.damage * dt * 10; combo = 0; if (player.health <= 0) gameOver = true; }
            }

            for (int i = particles.size()-1; i >= 0; i--) {
                float[] p = particles.get(i);
                p[0] += p[2] * dt; p[1] += p[3] * dt; p[4] -= dt;
                if (p[4] <= 0) particles.remove(i);
            }

            player.attackTimer -= dt;
            if (player.attackTimer <= 0) { player.attackTimer = 1.0f / player.attackSpeed; fire(); }
        }

        void spawnEnemy() {
            int side = random.nextInt(4);
            float x = side == 0 ? random.nextInt(getWidth()) : side == 1 ? getWidth()+50 : side == 2 ? random.nextInt(getWidth()) : -50;
            float y = side == 0 ? -50 : side == 1 ? random.nextInt(getHeight()) : side == 2 ? getHeight()+50 : random.nextInt(getHeight());
            Enemy e = new Enemy(x, y);
            e.maxHealth = 20 + wave * 10; e.health = e.maxHealth; e.speed = 80 + wave * 5;
            enemies.add(e);
        }

        void fire() {
            if (enemies.isEmpty()) return;
            Enemy t = enemies.get(0); float minD = Float.MAX_VALUE;
            for (Enemy e : enemies) { float d = (float)Math.sqrt(Math.pow(e.x-player.x,2)+Math.pow(e.y-player.y,2)); if (d < minD) { minD = d; t = e; } }
            float dx = t.x - player.x, dy = t.y - player.y, d = (float)Math.sqrt(dx*dx + dy*dy);
            projectiles.add(new Projectile(player.x, player.y, dx/d * 600, dy/d * 600, player.damage));
        }

        void addParticle(float x, float y, int color) {
            float angle = random.nextFloat() * (float)Math.PI * 2;
            float speed = 100 + random.nextFloat() * 200;
            particles.add(new float[]{x, y, (float)Math.cos(angle)*speed, (float)Math.sin(angle)*speed, 0.5f, Color.red(color), Color.green(color), Color.blue(color)});
        }

        void restart() {
            enemies.clear(); projectiles.clear(); particles.clear();
            player = new Player(getWidth()/2, getHeight()*0.7f);
            wave = 1; killCount = 0; score = 0; combo = 0; gameTime = 0; gameOver = false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            touchX = e.getX(); touchY = e.getY();
            touching = e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE;
            return true;
        }
    }

    static class Player { float x, y, speed = 400, health = 100, maxHealth = 100, damage = 25, attackSpeed = 2.5f, attackTimer = 0; Player(float x, float y) { this.x = x; this.y = y; } }
    static class Enemy { float x, y, size = 25, speed = 100, damage = 1, health = 30, maxHealth = 30; int color = Color.rgb(200, 60, 60); Enemy(float x, float y) { this.x = x; this.y = y; } }
    static class Projectile { float x, y, vx, vy, damage, life = 3; Projectile(float x, float y, float vx, float vy, float d) { this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.damage = d; } }
}
