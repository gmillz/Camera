package dev.gmillz.camera.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gmillz.camera.CamConfig
import dev.gmillz.camera.capturer.ImageCapturer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun providesImageCapturer(context: Application, camConfig: CamConfig): ImageCapturer {
        return ImageCapturer(context, camConfig)
    }

    @Provides
    @Singleton
    fun providesCamConfig(context: Application): CamConfig {
        return CamConfig(context)
    }
}