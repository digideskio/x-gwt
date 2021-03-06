package com.github.spirylics.xgwt.polymer;

import com.google.common.base.Strings;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.query.client.Function;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.query.client.Promise;
import com.google.gwt.user.client.History;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Dialog extends PolymerComponent implements HasOpenHandlers<Dialog>, HasCloseHandlers<Dialog> {
    @SuppressWarnings("ALL")
    @JsType(isNative = true)
    public interface ClosingReason {
        @JsProperty
        public boolean isCanceled();

        @JsProperty
        public boolean isConfirmed();

    }

    String dialogParameter;
    HandlerRegistration historyHandlerRegistration;
    Function onOpened;

    public interface Events {
        String OVERLAY_OPENED = "iron-overlay-opened";
        String OVERLAY_CLOSED = "iron-overlay-closed";
    }

    public Dialog(String html) {
        super("paper-dialog", html);
        setAttribute("entry-animation", "scale-up-animation");
        setAttribute("exit-animation", "fade-out-animation");
        setAttribute("with-backdrop", "");
    }

    void enableHistory() {
        if (this.historyHandlerRegistration == null) {
            this.historyHandlerRegistration = History.addValueChangeHandler(new ValueChangeHandler<String>() {
                @Override
                public void onValueChange(final ValueChangeEvent<String> event) {
                    overlay(hasHistoryDialogParameter(event.getValue())).done(new Function() {
                        @Override
                        public void f() {
                            if (isOpened()) {
                                OpenEvent.fire(Dialog.this, Dialog.this);
                            } else {
                                disableHistory();
                                CloseEvent.fire(Dialog.this, Dialog.this);
                            }
                        }
                    });
                }
            });
        }
        if (this.onOpened == null) {
            this.onOpened = new Function() {
                @Override
                public void f() {
                    if (isOpened()) {
                        addDialogParameter();
                    } else {
                        removeDialogParameter();
                    }
                }
            };
            onPropertyChange("opened", this.onOpened);
        }
    }

    void disableHistory() {
        if (this.historyHandlerRegistration != null) {
            this.historyHandlerRegistration.removeHandler();
            this.historyHandlerRegistration = null;
        }
        if (this.onOpened != null) {
            offPropertyChange("opened", this.onOpened);
            this.onOpened = null;
        }
    }

    public void setHistoryToken(String historyToken) {
        this.dialogParameter = "dialog=" + historyToken;
    }

    public Dialog open() {
        enableHistory();
        if (hasHistoryDialogParameter(History.getToken())) {
            overlay(true);
        } else {
            addDialogParameter();
        }
        return this;
    }

    public Dialog close() {
        if (hasHistoryDialogParameter(History.getToken())) {
            removeDialogParameter();
        } else {
            overlay(false);
        }
        return this;
    }

    public boolean isOpened() {
        return get("opened");
    }

    public ClosingReason getClosingReason() {
        return get("closingReason");
    }

    String getDialogParameter() {
        if (Strings.isNullOrEmpty(dialogParameter)) {
            throw new IllegalStateException("no history token defined!");
        }
        return dialogParameter;
    }

    boolean hasHistoryDialogParameter(String historyToken) {
        return Strings.nullToEmpty(historyToken).contains(getDialogParameter());
    }

    Dialog addDialogParameter() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                if (!hasHistoryDialogParameter(History.getToken())) {
                    History.newItem(History.getToken()
                            + (Strings.isNullOrEmpty(History.getToken()) ? "#" : ";")
                            + getDialogParameter(), true);
                }
            }
        });
        return this;
    }

    void removeDialogParameter() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                if (hasHistoryDialogParameter(History.getToken())) {
                    History.back();
                }
            }
        });
    }

    Promise overlay(final boolean opened) {
        final Promise.Deferred deferred = GQuery.Deferred();
        if (isOpened() == opened) {
            deferred.resolve(opened);
        } else {
            once(opened ? Events.OVERLAY_OPENED : Events.OVERLAY_CLOSED, new Function() {
                @Override
                public void f() {
                    deferred.resolve(opened);
                }
            });
            set("opened", opened);
        }
        return deferred.promise();
    }

    @Override
    public HandlerRegistration addOpenHandler(OpenHandler<Dialog> handler) {
        return addHandler(handler, OpenEvent.getType());
    }

    @Override
    public HandlerRegistration addCloseHandler(CloseHandler<Dialog> handler) {
        return addHandler(handler, CloseEvent.getType());
    }
}
