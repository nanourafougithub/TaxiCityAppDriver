package example.com.taxicityappdriver.model.datasources;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import example.com.taxicityappdriver.controller.WaitingTripAdapter;
import example.com.taxicityappdriver.controller.WaitingTripViewHolder;
import example.com.taxicityappdriver.model.helpers.Helpers;
import example.com.taxicityappdriver.model.helpers.LocationHelper;
import example.com.taxicityappdriver.entities.Driver;
import example.com.taxicityappdriver.entities.Trip;
import example.com.taxicityappdriver.model.backend.ActionCallBack;
import example.com.taxicityappdriver.model.backend.BackEnd;
import example.com.taxicityappdriver.model.backend.CheckBoleanMethodCondition;
import example.com.taxicityappdriver.model.backend.NotifyDataChange;

public class FireBase_Manager implements BackEnd<String> {
    private static FirebaseDatabase db = FirebaseDatabase.getInstance();
    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static DatabaseReference refTrips = db.getReference("Trips");
    private static DatabaseReference refDriver = db.getReference("Drivers");
    private final String TAG = "firebaseManager";
    private static Driver currentDriver;
    private static Trip currentTrip;
    private static ChildEventListener tripRefChildEventListener;
    private static ChildEventListener driverRefChildEventListener;
    private static ChildEventListener currentDriverChildEventListener;


    private static ArrayList<Driver> drivers = new ArrayList<>();
    private static ArrayList<Trip> Trips = new ArrayList<>();


    public void addTrip(final Trip trip, final ActionCallBack<String> action) {
        String idTrip = trip.getKey();


        //Keep the same key when updating
        if (idTrip == null)
            idTrip = refTrips.push().getKey();
        else
            trip.setKey(null);

        Log.i(TAG, "addTrip: " + idTrip);

        final String finalIdTrip = idTrip;
        refTrips.child(idTrip).setValue(trip).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                action.onSuccess(finalIdTrip);
                action.onProgress("Adding trip in progress...", 100);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                action.onFailure(e);
                action.onProgress("Failed to add the trips...", 100);
            }
        });
    }


    public void removeTrip(final String key, final ActionCallBack<String> action) {

        Log.i(TAG, "removeTrip: " + key);
        refTrips.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final Trip value = dataSnapshot.getValue(Trip.class);

                if (value == null) {
                    action.onFailure(new Exception("We can't find the request trip"));
                } else {
                    refTrips.child(key).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            action.onSuccess(key);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            action.onFailure(e);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                action.onFailure(databaseError.toException());
            }
        });

    }


    /*public void updateTrip(final Trip toUpdate, final ActionCallBack<String> action) {
        final String key = toUpdate.getKey();


        Log.e(TAG, "Update Started");
        Log.i(TAG, "updateTrip - key :" + key);
        Log.i(TAG, "updateTrip: - Destination :" + toUpdate.getDestinationAddress());
        removeTrip(key, new ActionCallBack<String>() {
            @Override
            public void onSuccess(String obj) {
                addTrip(toUpdate, action);
            }

            @Override
            public void onFailure(Exception exception) {
                action.onFailure(exception);
            }

            @Override
            public void onProgress(String status, double percent) {
                action.onProgress(status, percent);
            }
        });
    }*/

    public void updateTrip(final Trip toUpdate, final ActionCallBack<String> action) {
        Log.e(TAG, "Update Started");
        Log.i(TAG, "updateTrip - key :" + toUpdate.getKey());
        Log.i(TAG, "updateTrip: - Destination :" + toUpdate.getDestinationAddress());

        final String finalIdTrip = toUpdate.getKey();
        refTrips.child(finalIdTrip).setValue(toUpdate).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                action.onSuccess(finalIdTrip);
                action.onProgress("Adding trip in progress...", 100);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                action.onFailure(e);
                action.onProgress("Failed to add the trips...", 100);
            }
        });

    }





    @Override
    public void getTrip(final String key, final ActionCallBack<Trip> action) {

        final Trip[] trip = {null};
        refTrips.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                trip[0] = dataSnapshot.getValue(Trip.class);

                if (trip[0] == null) {

                    action.onFailure(new Exception("We can't find the Trip"));
                } else {
                    trip[0].setKey(key); //A remettre
                    action.onSuccess(trip[0]);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
                action.onFailure(databaseError.toException());
            }
        });

    }

    @Override
    public void addDriver(final Driver driver, final ActionCallBack<String> action) {
        String email = EncodeString(driver.getEmail());


        Log.i(TAG, "addDriver: ");
        final String finalEmailDriver = email;
        //Test if the driver already exist
        getDriver(email, new ActionCallBack<Driver>() {
            @Override
            public void onSuccess(Driver obj) {
                action.onFailure(new Exception("Driver already Exist !"));
                Log.i(TAG, "onSuccess: ");
            }

            @Override
            public void onFailure(Exception exception) {
                //If the driver don't exits
                refDriver.child(finalEmailDriver).setValue(driver).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        action.onSuccess(finalEmailDriver);
                        action.onProgress("Adding driver in progress...", 100);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        action.onFailure(e);
                        action.onProgress("Failed to add the driver...", 100);
                    }
                });

                Log.i(TAG, "onFailure: ");

            }

            @Override
            public void onProgress(String status, double percent) {

            }
        });

    }

    @Override
    public void removeDriver(final String email, final ActionCallBack<String> action) {

        final String emailKey = EncodeString(email);

        refDriver.child(emailKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final Driver value = dataSnapshot.getValue(Driver.class);

                if (value == null) {
                    action.onFailure(new Exception("We can't find the request trip"));
                } else {
                    refTrips.child(emailKey).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            action.onSuccess(emailKey);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            action.onFailure(e);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                action.onFailure(databaseError.toException());
            }
        });
    }

    //By ID
    @Override
    public void getDriver(final String email, final ActionCallBack<Driver> action) {
        Log.i(TAG, "getDriver: ");
        final Driver[] drivers = {null};
        final String emailKey = EncodeString(email);
        refDriver.child(emailKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                drivers[0] = dataSnapshot.getValue(Driver.class);

                if (drivers[0] == null) {
                    Log.i(TAG, "onDataChange: FAILURE");

                    action.onFailure(new Exception("We can't find the Driver"));
                } else {
                    Log.i(TAG, "onDataChange: SUCCESS");
                    Log.i(TAG, emailKey);
                    drivers[0].setEmail(DecodeString(emailKey));
                    action.onSuccess(drivers[0]);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
                action.onFailure(databaseError.toException());
            }
        });

    }

    @Override
    public void updateDriver(final Driver toUpdate, final ActionCallBack<String> action) {
        final String email = EncodeString(toUpdate.getEmail());

        Log.e(TAG, "Update Started");
        removeTrip(email, new ActionCallBack<String>() {
            @Override
            public void onSuccess(String obj) {
                addDriver(toUpdate, action);
            }

            @Override
            public void onFailure(Exception exception) {
                action.onFailure(exception);
            }

            @Override
            public void onProgress(String status, double percent) {
                action.onProgress(status, percent);
            }
        });

    }

    @Override
    public void signUp(String email, String password, final ActionCallBack<Object> action) {
        Log.i(TAG, "signUp: ");
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.i(TAG, "onComplete: SUCESSS");
                    action.onSuccess(mAuth.getCurrentUser());
                } else {
                    action.onFailure(task.getException());
                    Log.i(TAG, "onComplete: FAILURE");
                    Log.i(TAG, task.getException().getMessage());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(TAG, "onFailure: ");
                Log.i(TAG, e.getMessage());
                action.onFailure(e);


            }
        });

    }

    @Override
    public void signIn(String email, String password, final ActionCallBack<Object> action) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.i(TAG, "onComplete: SUCESSS");
                    action.onSuccess(mAuth.getCurrentUser());
                    getDriver(mAuth.getCurrentUser().getEmail(), new ActionCallBack<Driver>() {
                        @Override
                        public void onSuccess(Driver obj) {
                            currentDriver = obj;
                        }

                        @Override
                        public void onFailure(Exception exception) {

                        }

                        @Override
                        public void onProgress(String status, double percent) {

                        }
                    });
                } else {
                    action.onFailure(task.getException());
                    Log.i(TAG, "onComplete: FAILURE");
                    Log.i(TAG, task.getException().getMessage());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i(TAG, "onFailure: ");
                Log.i(TAG, e.getMessage());
                action.onFailure(e);


            }
        });


    }

    @Override
    public void signOut() {
        mAuth.signOut();
    }

    @Override
    public void forgotPassword(String email, final ActionCallBack<String> action) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email sent.");
                            action.onSuccess("Email sent.");
                        } else
                            action.onFailure(task.getException());
                    }
                });
    }

    @Override
    public boolean isSigned() {
        return mAuth.getCurrentUser() != null;
    }

    @Override
    public Driver getCurrentDriver() {
        if (currentDriver == null)
            NotifyCurrentDriver(new NotifyDataChange<Driver>() {
                @Override
                public void OnDataChanged(Driver obj) {
                    currentDriver = obj;
                }

                @Override
                public void onFailure(Exception exception) {

                }
            });


        return currentDriver;
    }


    private void NotifyCurrentDriver(final NotifyDataChange<Driver> notifyDataChange) {

        if (notifyDataChange == null)
            return;

        final String email = EncodeString(mAuth.getCurrentUser().getEmail());
        if (currentDriverChildEventListener != null) {
            refDriver.removeEventListener(currentDriverChildEventListener);
            return;
        }
        currentDriver = null;

        currentDriverChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {


                if (EncodeString(dataSnapshot.getKey()).equals(email)) {
                    Driver driver = dataSnapshot.getValue(Driver.class);
                    driver.setEmail(email);
                    notifyDataChange.OnDataChanged(driver);
                    Log.i(TAG, "onChildAdded: " + currentDriver.getLastName());
                }


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (EncodeString(dataSnapshot.getKey()).equals(email)) {
                    Driver driver = dataSnapshot.getValue(Driver.class);
                    driver.setEmail(email);
                    currentDriver = driver;
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        refDriver.addChildEventListener(currentDriverChildEventListener);


    }


    @Override
    public void deleteCurrentUser() {
        FirebaseUser user = mAuth.getCurrentUser();

        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User account deleted.");
                        }
                    }
                });
    }


    private static String EncodeString(String string) {
        return string.replace(".", ",");
    }

    private static String DecodeString(String string) {
        return string.replace(",", ".");
    }


    /*public void notifyToTripList(final NotifyDataChange<List<Trip>> notifyDataChange) {
        if (notifyDataChange != null) {

            if (tripRefChildEventListener != null) {
                notifyDataChange.onFailure(new Exception("first unNotify student list"));
                return;
            }
            Trips.clear();

            tripRefChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Trip trip = dataSnapshot.getValue(Trip.class);
                    String key = dataSnapshot.getKey();
                    trip.setKey(key);
                    Trips.add(trip);


                    notifyDataChange.OnDataChanged(Trips);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Trip trip = dataSnapshot.getValue(Trip.class);
                    String key = dataSnapshot.getKey();
                    trip.setKey(key);



                    for (int i = 0; i < Trips.size(); i++) {
                        if (Trips.get(i).getKey().equals(key)) {
                            Trips.set(i, trip);
                            break;
                        }
                    }
                    notifyDataChange.OnDataChanged(Trips);
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Trip trip = dataSnapshot.getValue(Trip.class);
                    String key = dataSnapshot.getKey();
                    trip.setKey(key);

                    for (int i = 0; i < Trips.size(); i++) {
                        if (Trips.get(i).getKey() == key) {
                            Trips.remove(i);
                            break;
                        }
                    }
                    notifyDataChange.OnDataChanged(Trips);
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    notifyDataChange.onFailure(databaseError.toException());
                }
            };
            refTrips.addChildEventListener(tripRefChildEventListener);
        }
    }*/

    public void notifyToTripList(final NotifyDataChange<List<Trip>> notifyDataChange, final CheckBoleanMethodCondition<Trip> condition) {
        if (notifyDataChange != null) {

            if (tripRefChildEventListener != null) {
                notifyDataChange.onFailure(new Exception("first unNotify student list"));
                return;
            }
            Trips.clear();

            tripRefChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Trip trip = dataSnapshot.getValue(Trip.class);
                    String key = dataSnapshot.getKey();
                    trip.setKey(key);

                    boolean contains = false;
                    if (condition == null || condition.isTrue(trip)) {



                        for (int i = 0; i < Trips.size(); i++) {
                            if (Trips.get(i).getKey().equals(key)) {
                                Trips.set(i, trip);
                                contains = true;
                                break;
                            }
                        }

                        if(!contains)
                            Trips.add(trip);

                        notifyDataChange.OnDataChanged(Trips);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Trip trip = dataSnapshot.getValue(Trip.class);
                    String key = dataSnapshot.getKey();
                    trip.setKey(key);


                    if (condition == null || condition.isTrue(trip)) {

                        for (int i = 0; i < Trips.size(); i++) {
                            if (Trips.get(i).getKey().equals(key)) {
                                Trips.set(i, trip);
                                break;
                            }
                        }
                        notifyDataChange.OnDataChanged(Trips);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Trip trip = dataSnapshot.getValue(Trip.class);
                    String key = dataSnapshot.getKey();
                    trip.setKey(key);

                    if (condition == null || condition.isTrue(trip)) {

                        for (int i = 0; i < Trips.size(); i++) {
                            if (Trips.get(i).getKey() == key) {
                                Trips.remove(i);
                                break;
                            }
                        }
                        notifyDataChange.OnDataChanged(Trips);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    notifyDataChange.onFailure(databaseError.toException());
                }
            };
            refTrips.addChildEventListener(tripRefChildEventListener);
        }
    }


    public void notifyToTripListAll(final NotifyDataChange<List<Trip>> notifyDataChange) {
        if (tripRefChildEventListener != null)
            stopNotifyToTripList();
        notifyToTripList(notifyDataChange, null);
    }


    public void notifyToTripListWaiting(final NotifyDataChange<List<Trip>> notifyDataChange) {
        if (tripRefChildEventListener != null)
            stopNotifyToTripList();
        notifyToTripList(notifyDataChange, new CheckBoleanMethodCondition<Trip>() {
            @Override
            public boolean isTrue(Trip obj) {

                //Prevent Delete Cell on refresh
                boolean flag = false;
                if(WaitingTripAdapter.isBusyDriver() && obj.getKey().equals(WaitingTripViewHolder.getBusyKey()) &&  obj.getDriverEmail().equals(getCurrentDriver().getEmail()) )
                    flag = true;

                return flag || obj.getStatusAsEnum() == Trip.TripStatus.AVAILABLE ;
            }
        });
    }

    public void notifyToTripListFinished(final NotifyDataChange<List<Trip>> notifyDataChange) {
        if (tripRefChildEventListener != null)
            stopNotifyToTripList();
        notifyToTripList(notifyDataChange, new CheckBoleanMethodCondition<Trip>() {
            @Override
            public boolean isTrue(Trip obj) {
                return obj.getStatusAsEnum() == Trip.TripStatus.FINISHED;
            }
        });
    }

    public void notifyToTripListByDriver(final String driverEmail, final NotifyDataChange<List<Trip>> notifyDataChange) {
        if (tripRefChildEventListener != null)
            stopNotifyToTripList();
        notifyToTripList(notifyDataChange, new CheckBoleanMethodCondition<Trip>() {
            @Override
            public boolean isTrue(Trip obj) {
                return obj.getDriverEmail().equals(driverEmail);
            }
        });
    }

    public void notifyToTripListWaitingByCity(final String destinationCity, final NotifyDataChange<List<Trip>> notifyDataChange) {
        if (tripRefChildEventListener != null)
            stopNotifyToTripList();
        notifyToTripList(notifyDataChange, new CheckBoleanMethodCondition<Trip>() {
            @Override
            public boolean isTrue(Trip obj) {
                return obj.getDestinationCity().equals(destinationCity) && obj.getStatusAsEnum() == Trip.TripStatus.AVAILABLE;
            }
        });
    }

    public void notifyToTripListWaitingByDistance(final int distanceInKm, final NotifyDataChange<List<Trip>> notifyDataChange) {
        if (tripRefChildEventListener != null)
            stopNotifyToTripList();
        notifyToTripList(notifyDataChange, new CheckBoleanMethodCondition<Trip>() {
            @Override
            public boolean isTrue(Trip obj) {
                return LocationHelper.calculDistanceFromYou(obj, getCurrentDriver()) / 1000 < distanceInKm;
            }
        });
    }


    public void notifyToTripListByAmounth(final double min, final double max, final NotifyDataChange<List<Trip>> notifyDataChange) {
        if (tripRefChildEventListener != null)
            stopNotifyToTripList();
        notifyToTripList(notifyDataChange, new CheckBoleanMethodCondition<Trip>() {
            @Override
            public boolean isTrue(Trip obj) {
                double price = LocationHelper.calculTripDistance(obj) / 1000 * Helpers.PRICE_BY_KM;
                return (obj.getStatusAsEnum() == Trip.TripStatus.FINISHED && price > min && price < max);
            }
        });
    }


    //Boefore
    public void notifyToTripListBeforeDate(final Date date, final NotifyDataChange<List<Trip>> notifyDataChange) {
        if (tripRefChildEventListener != null)
            stopNotifyToTripList();
        notifyToTripList(notifyDataChange, new CheckBoleanMethodCondition<Trip>() {
            @Override
            public boolean isTrue(Trip obj) {
                return (obj.getStartingHourAsDate().before(date));
            }
        });
    }


    public void stopNotifyToTripList() {
        if (tripRefChildEventListener != null) {
            refTrips.removeEventListener(tripRefChildEventListener);
            tripRefChildEventListener = null;
        }
    }


}

