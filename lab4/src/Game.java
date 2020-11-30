public class Game {

    private final int width;
    private final int height;
    private final int food_static;
    private final float food_per_player;
    private final int state_delay_ms;
    private final float dead_food_prob;
    private final int ping_delay_ms;
    private final int node_died_timeout_ms;

    Game(int _width, int _height, int _food_static, float _food_per_player,
            int _state_delay_ms, float _dead_food_prob, int _ping_delay_ms, int _node_died_timeout_ms){
        width = _width;
        height = _height;
        food_static = _food_static;
        food_per_player = _food_per_player;
        state_delay_ms = _state_delay_ms;
        dead_food_prob = _dead_food_prob;
        ping_delay_ms = _ping_delay_ms;
        node_died_timeout_ms = _node_died_timeout_ms;

    }




}
