package Identity.Server;

/**
 * State information used for two phase commit process
 */
public class CommitState {

    public enum State {INIT, WAIT, ABORT, COMMIT, READY};
    private State currentState;

    public CommitState(){
        this.currentState = State.INIT;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void setCurrentState(State nextState) {
        this.currentState = nextState;
    }

    public void setCurrentState(int nextState) {
        switch(nextState) {
            case 1: this.currentState = State.INIT;
                break;
            case 2: this.currentState = State.WAIT;
                break;
            case 3: this.currentState = State.ABORT;
                break;
            case 4: this.currentState = State.COMMIT;
                break;
            case 5: this.currentState = State.READY;
        }
    }

    public static int stateToInt(State state) {
        switch(state) {
            case INIT: return 1;
            case WAIT: return 2;
            case ABORT: return 3;
            case COMMIT: return 4;
            case READY: return 5;
        }
        return -1;
    }


}
