package com.brimstonetower.game.screens;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.brimstonetower.game.TheBrimstoneTowerGame;
import com.brimstonetower.game.gui.Button;
import com.brimstonetower.game.managers.AssetManager;

public class MenuScreen implements Screen, GestureDetector.GestureListener
{
    private Button startButton;
    private Button highScoreButton;
    private Button exitGameButton;
    private Color _buttonColor = new Color(0.6f, 0.07f, 0.07f, 1f);

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera guiCamera;
    private BitmapFont _font;
    private final String _gameTitle;
    private final String _versionString;
    private Vector2 _titlePosition;
    private Sprite bg;
    Vector2 crop = new Vector2();
    public MenuScreen()
    {
        TextureRegion buttonRegion = new TextureRegion(AssetManager.getGuiTexture("menuButton"),0,48,128,64);
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        int buttonWidth = (int)(w * 0.18f);
        int buttonHeight = (int)(h * 0.14f);
        setupBackground(w,h);

        startButton = new Button((w/2 - (buttonWidth / 2)),(int) (h * 0.55f - buttonHeight), buttonRegion,buttonWidth,buttonHeight, "Start Game");
        highScoreButton = new Button((w/2 - (buttonWidth / 2)),(int) (h * 0.7f - buttonHeight), buttonRegion,buttonWidth,buttonHeight, "High scores");
        exitGameButton = new Button((w/2 - (buttonWidth / 2)),(int) (h * 0.85f - buttonHeight), buttonRegion,buttonWidth,buttonHeight, "Exit Game");
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        guiCamera = new OrthographicCamera(w, h);
        guiCamera.setToOrtho(true, w, h);

        _versionString=TheBrimstoneTowerGame.version + " " + TheBrimstoneTowerGame.versionState;
        _gameTitle = "The Brimstone Tower";
        _font = AssetManager.getFont("description");

        GlyphLayout layout = new GlyphLayout();
        layout.setText(_font,_gameTitle);
        _titlePosition = new Vector2(Gdx.graphics.getWidth() / 2 - layout.width/2, Gdx.graphics.getHeight() * 0.15f);
        Gdx.input.setInputProcessor(new GestureDetector(this));
    }

    private void setupBackground(int screenWidth, int screenHeight)
    {
        TextureRegion bgRegion = new TextureRegion(AssetManager.getGuiTexture("background"),0,0,1280,960);
        bgRegion.flip(false, true);
        bg = new Sprite(bgRegion);
        float bgScale = bg.getWidth()/bg.getHeight();
        float screenScale = (float)screenWidth/(float)screenHeight;

        float scale= 1;
        if(bgScale>screenScale)
        {
            scale = bg.getHeight()/(float)screenHeight;
            crop.x = (bg.getWidth()/scale)-screenWidth;
        }
        else if(bgScale<screenScale)
        {
            scale = bg.getWidth()/(float)screenWidth;
            crop.y = (bg.getHeight()/scale)-screenHeight;
        }
        else
        {
            scale = bg.getWidth()/(float)screenWidth;
        }
        int x=(int)crop.x;
        int y=(int)crop.y;
        int bW = (int)(bg.getWidth()/scale);
        int bH = (int)(bg.getHeight()/scale);


        bg.setSize(bW, bH);
        bg.setPosition(-x, -y);
    }
    @Override
    public void render(float v)
    {
        GlyphLayout layout = new GlyphLayout();
        layout.setText(_font,_versionString);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(guiCamera.combined);
        shapeRenderer.setProjectionMatrix(guiCamera.combined);
        batch.begin();
        bg.draw(batch);
        _font.draw(batch, _gameTitle, _titlePosition.x, _titlePosition.y);
        _font.draw(batch,
                TheBrimstoneTowerGame.version + " " + TheBrimstoneTowerGame.versionState,
                Gdx.graphics.getWidth() - (layout.width*1.1f),
                Gdx.graphics.getHeight()-layout.height*1.1f);

        batch.end();
        guiCamera.update();



        startButton.draw(batch, shapeRenderer);
        highScoreButton.draw(batch, shapeRenderer);
        exitGameButton.draw(batch, shapeRenderer);

    }

    @Override
    public boolean touchDown(float v, float v2, int i, int i2)
    {
        return false;
    }


    @Override
    public boolean tap(float x, float y, int i, int i2)
    {
        if (startButton.isTouched(x, y))
        {
            TheBrimstoneTowerGame.getGameInstance().setScreen(new StoryScreen());
        }
        else if (highScoreButton.isTouched(x, y))
        {
            TheBrimstoneTowerGame.getGameInstance().setScreen(new HighScoreScreen());
        }
        else if (exitGameButton.isTouched(x, y))
        {
            Gdx.app.exit();
        }
        return true;
    }

    @Override
    public boolean longPress(float v, float v2)
    {
        return false;
    }

    @Override
    public boolean fling(float v, float v2, int i)
    {
        return false;
    }

    @Override
    public boolean pan(float v, float v2, float v3, float v4)
    {
        return false;
    }

    @Override
    public boolean panStop(float v, float v2, int i, int i2)
    {
        return false;
    }

    @Override
    public boolean zoom(float v, float v2)
    {
        return false;
    }

    @Override
    public boolean pinch(Vector2 vector2, Vector2 vector22, Vector2 vector23, Vector2 vector24)
    {
        return false;
    }

    @Override
    public void pinchStop() {

    }


    @Override
    public void resize(int i, int i2)
    {


    }

    @Override
    public void show()
    {

    }

    @Override
    public void hide()
    {

    }

    @Override
    public void pause()
    {

    }

    @Override
    public void resume()
    {

    }

    @Override
    public void dispose()
    {
        batch.dispose();
        shapeRenderer.dispose();

    }
}
