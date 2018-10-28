import com.google.inject.AbstractModule
import services._


/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule {

    override def configure() = {

        // Set for Guice which implementations to choose for the repositories
        // in order to allow their injection into controllers (controllers should declare repo's Trait as parameter)
        // when Play tries to instantiate a new Controller
        bind(classOf[PropertyRepository]).to(classOf[PropertyRepositoryImpl])
        bind(classOf[PriceRepository]).to(classOf[PriceRepositoryImpl])
    }

}
